package com.aiagentteam.app.agents

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.aiagentteam.app.accessibility.AgentAccessibilityService
import com.aiagentteam.app.keyboard.SampleStore
import kotlinx.coroutines.delay

class Orchestrator(private val ctx: Context) {
    val statuses = mutableStateMapOf<AgentRole, AgentStatus>()
    val log = mutableStateListOf<LogEntry>()
    val running = mutableStateOf(false)
    val draft = mutableStateOf("")
    private var last: TaskInput? = null

    init { reset() }

    fun reset() {
        AgentRole.values().forEach { statuses[it] = AgentStatus.WAITING }
        log.clear()
    }

    private fun say(role: AgentRole, msg: String) { log.add(LogEntry(role, msg)) }

    private fun clean(s: String) =
        s.replace(Regex("[\\p{So}\\p{Cs}\\uFE0F\\u200D]"), "").trim().trim('"')

    private suspend fun step(role: AgentRole, work: suspend () -> String): String {
        statuses[role] = AgentStatus.WORKING
        try {
            val r = work()
            say(role, r)
            statuses[role] = AgentStatus.COMPLETED
            return r
        } catch (e: Exception) {
            say(role, "خطأ: ${e.message}")
            statuses[role] = AgentStatus.FAILED
            throw e
        }
    }

    suspend fun run(task: TaskInput) {
        if (running.value) return
        running.value = true
        reset()
        draft.value = ""
        last = task
        try {
            if (!Llm.ready(ctx)) {
                say(AgentRole.ANALYST, "لا يوجد نموذج. استورده من قسم النموذج أولا")
                statuses[AgentRole.ANALYST] = AgentStatus.FAILED
                return
            }
            val samples = SampleStore.recent(ctx, 25)
            val examples = samples.joinToString("\n") { "- $it" }

            val analysis = step(AgentRole.ANALYST) {
                Llm.ask(ctx, "حلل هذه المهمة في سطرين: ما المطلوب بالضبط وما نبرة الرسالة؟\nالتطبيق: ${task.app}\nالشخص: ${task.person}\nالمطلوب: ${task.instruction}")
            }
            val context = step(AgentRole.PERSON) {
                if (task.personInfo.isBlank()) "لا توجد معلومات إضافية عن ${task.person}"
                else Llm.ask(ctx, "استخرج في ثلاثة أسطر ما يفيد في الحديث مع ${task.person}:\n${task.personInfo}")
            }
            val style = step(AgentRole.STYLE) {
                if (samples.isEmpty()) "لا توجد عينات كتابة. سأكتب بأسلوب عام"
                else Llm.ask(ctx, "هذه رسائل كتبها شخص:\n$examples\nصف أسلوبه في أربعة أسطر: اللهجة، طول الرسائل، الاختصارات والأخطاء المعتادة، علامات الترقيم، وخلط العربية باللاتينية.")
            }
            val plan = step(AgentRole.PLANNER) {
                Llm.ask(ctx, "ضع خطة من ثلاث نقاط قصيرة لتنفيذ هذه المهمة:\n$analysis\n$context")
            }
            var text = clean(step(AgentRole.EXECUTOR) {
                Llm.ask(ctx, "اكتب رسالة واحدة فقط إلى ${task.person} عبر ${task.app}.\nالمطلوب: ${task.instruction}\nالسياق: $context\nالخطة: $plan\nملاحظات الأسلوب: $style\nأمثلة من كتابة المستخدم، قلدها في اللهجة والطول والاختصارات:\n$examples\nأخرج نص الرسالة فقط، بلا شرح ولا اقتباس ولا رموز تعبيرية.")
            })
            val review = step(AgentRole.REVIEWER) {
                Llm.ask(ctx, "راجع هذه الرسالة:\n$text\nملاحظات الأسلوب: $style\nإن كانت مناسبة للأسلوب والمطلوب أجب بكلمة OK فقط، وإلا اذكر المشكلة في سطر واحد.")
            }
            step(AgentRole.CORRECTOR) {
                if (review.trim().uppercase().startsWith("OK")) "لا توجد تصحيحات"
                else {
                    text = clean(Llm.ask(ctx, "أعد كتابة الرسالة بعد معالجة هذه الملاحظة.\nالرسالة: $text\nالملاحظة: $review\nملاحظات الأسلوب: $style\nأخرج نص الرسالة فقط بلا رموز تعبيرية."))
                    "تم تعديل الرسالة"
                }
            }
            draft.value = text
            say(AgentRole.EXECUTOR, "المسودة جاهزة للتنفيذ")
            if (task.mode == RunMode.AUTO) deliver()
        } catch (e: Exception) {
        } finally {
            running.value = false
        }
    }

    suspend fun deliver() {
        val t = last ?: return
        val text = draft.value
        if (text.isBlank()) return
        val s = AgentAccessibilityService.instance
        if (s == null) { say(AgentRole.EXECUTOR, "خدمة الوصول غير مفعلة"); return }
        statuses[AgentRole.EXECUTOR] = AgentStatus.WORKING
        ctx.packageManager.getLaunchIntentForPackage(t.pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { ctx.startActivity(it) }
        delay(5000)
        if (t.person.isNotBlank()) {
            val found = s.clickByLabel(listOf(t.person))
            say(AgentRole.EXECUTOR, if (found) "تم فتح ${t.person}" else "لم أجد ${t.person} على الشاشة. افتح المحادثة بنفسك")
            delay(3000)
        }
        val typed = s.typeText(text)
        say(AgentRole.EXECUTOR, if (typed) "كتبت الرسالة في الحقل" else "لم أجد حقل كتابة")
        if (typed && last?.mode == RunMode.AUTO) {
            delay(600)
            val sent = s.clickByLabel(listOf("send", "إرسال", "ارسال"))
            say(AgentRole.EXECUTOR, if (sent) "تم الإرسال" else "لم أجد زر الإرسال. اضغطه بنفسك")
        } else if (typed) {
            say(AgentRole.EXECUTOR, "الرسالة في الحقل. اضغط إرسال بنفسك")
        }
        statuses[AgentRole.EXECUTOR] = AgentStatus.COMPLETED
    }
}
