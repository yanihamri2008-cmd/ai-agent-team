package com.aiagentteam.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aiagentteam.app.accessibility.AgentAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AccessTestPanel(pkg: String) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var on by remember { mutableStateOf(AgentAccessibilityService.instance != null) }
    var report by remember { mutableStateOf("") }

    fun launchTarget() {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (i != null) ctx.startActivity(i)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("خدمة الوصول: " + if (on) "مفعلة" else "غير مفعلة",
            style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text("فتح الإعدادات") }
            OutlinedButton(onClick = { on = AgentAccessibilityService.instance != null }) { Text("تحديث") }
        }
        OutlinedButton(
            enabled = on && pkg.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    launchTarget()
                    delay(8000)
                    val s = AgentAccessibilityService.instance
                    report = if (s == null) "الخدمة غير متصلة"
                    else if (s.typeText("اختبار")) "تمت الكتابة في الحقل" else "لم أجد حقل كتابة"
                }
            }
        ) { Text("اختبار الكتابة (8 ثوان)") }
        OutlinedButton(
            enabled = on && pkg.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    launchTarget()
                    delay(8000)
                    report = AgentAccessibilityService.instance?.dump() ?: "الخدمة غير متصلة"
                }
            }
        ) { Text("عرض ما تراه الخدمة (8 ثوان)") }
        if (report.isNotBlank()) Text(report)
    }
}
