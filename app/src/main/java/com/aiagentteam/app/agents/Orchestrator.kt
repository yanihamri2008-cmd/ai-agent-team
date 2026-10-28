package com.aiagentteam.app.agents

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.aiagentteam.app.keyboard.SampleStore
import kotlinx.coroutines.delay

class Orchestrator(private val ctx: Context) {
    val statuses = mutableStateMapOf<AgentRole, AgentStatus>()
    val log = mutableStateListOf<LogEntry>()
    val running = mutableStateOf(false)

    init { reset() }

    fun reset() {
        AgentRole.values().forEach { statuses[it] = AgentStatus.WAITING }
        log.clear()
    }

    private fun say(role: AgentRole, msg: String) {
        log.add(LogEntry(role, msg))
    }

    private suspend fun step(role: AgentRole, work: () -> String) {
        statuses[role] = AgentStatus.WORKING
        delay(400)
        try {
            say(role, work())
            statuses[role] = AgentStatus.COMPLETED
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
        try {
            step(AgentRole.ANALYST) {
                "تم استلام المهمة في ${task.app}: ${task.instruction.take(80)}"
            }
            step(AgentRole.PERSON) {
                if (task.personInfo.isBlank()) "لا توجد معلومات إضافية عن ${task.person}"
                else "تم تحميل سياق ${task.person} (${task.personInfo.length} حرف)"
            }
            step(AgentRole.STYLE) {
                val n = SampleStore.count(ctx)
                if (n == 0) "لا توجد عينات كتابة بعد. اكتب بلوحة المفاتيح الخاصة بالتطبيق"
                else "عدد العينات المحفوظة: $n (التحليل لم يُربط بعد)"
            }
            step(AgentRole.PLANNER) {
                "الخطة: فتح ${task.app}، البحث عن ${task.person}، تجهيز الرسالة"
            }
            step(AgentRole.EXECUTOR) {
                if (task.mode == RunMode.REVIEW) "جاهز. بانتظار موافقتك (التنفيذ داخل التطبيقات لم يُربط بعد)"
                else "الوضع التلقائي مختار. خدمة الوصول غير مفعلة بعد"
            }
            step(AgentRole.REVIEWER) { "تمت المراجعة. النموذج غير مربوط بعد" }
            step(AgentRole.CORRECTOR) { "لا توجد تصحيحات" }
        } catch (e: Exception) {
            // الحالة سُجلت مسبقا
        } finally {
            running.value = false
        }
    }
}
