package com.aiagentteam.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiagentteam.app.agents.*
import com.aiagentteam.app.keyboard.SampleStore
import kotlinx.coroutines.launch

private const val IG_URL = "https://www.instagram.com/j_120_x?mdxt=dDJvdjU4YW04Ym0x"

private fun statusColor(s: AgentStatus?): Color = when (s) {
    AgentStatus.WORKING -> Color(0xFFFFB84D)
    AgentStatus.COMPLETED -> Color(0xFF4CD08A)
    AgentStatus.FAILED -> Color(0xFFFF6B6B)
    else -> Color(0xFF6B7388)
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun MainScreen() {
    val ctx = LocalContext.current.applicationContext
    val orch = remember { Orchestrator(ctx) }
    val apps = remember { InstalledApps.list(ctx) }
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var person by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var auto by remember { mutableStateOf(false) }
    var samples by remember { mutableStateOf(SampleStore.count(ctx)) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showPicker) {
            AppPickerDialog(apps, onPick = { app = it.label; pkg = it.pkg; showPicker = false },
                onDismiss = { showPicker = false })
        }
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { pad ->
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                        Text("فريق الذكاء الاصطناعي", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("سبعة وكلاء يتعاونون على مهمتك",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Section("المهمة") {
                        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (pkg.isEmpty()) "اختيار التطبيق" else "التطبيق: $app")
                        }
                        OutlinedTextField(person, { person = it }, label = { Text("اسم الشخص") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(info, { info = it }, label = { Text("معلومات عن الشخص") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(instruction, { instruction = it }, label = { Text("ماذا تريد") },
                            modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (auto) "الوضع التلقائي" else "وضع المراجعة", Modifier.weight(1f))
                            Switch(auto, { auto = it })
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    orch.run(TaskInput(app, person, info, instruction,
                                        if (auto) RunMode.AUTO else RunMode.REVIEW))
                                    samples = SampleStore.count(ctx)
                                }
                            },
                            enabled = !orch.running.value && instruction.isNotBlank() && pkg.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) { Text("تشغيل الفريق", fontWeight = FontWeight.SemiBold) }
                    }
                }
                item {
                    Section("الوكلاء") {
                        AgentRole.values().forEach { role ->
                            val st = orch.statuses[role]
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(statusColor(st)))
                                Spacer(Modifier.width(10.dp))
                                Text(role.title, Modifier.weight(1f))
                                Text(st?.label ?: "", color = statusColor(st), fontSize = 13.sp)
                            }
                        }
                    }
                }
                item {
                    Section("سجل العمليات") {
                        if (orch.log.isEmpty()) {
                            Text("لا توجد عمليات بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        orch.log.toList().forEach { e ->
                            Column {
                                Text(e.role.title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(e.message, fontSize = 14.sp)
                            }
                        }
                    }
                }
                item {
                    Section("أسلوب الكتابة") {
                        Text("العينات المحفوظة: $samples")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { samples = SampleStore.count(ctx) }) { Text("تحديث") }
                            OutlinedButton(onClick = { SampleStore.clear(ctx); samples = 0 }) { Text("مسح العينات") }
                        }
                    }
                }
                item { Section("خدمة الوصول") { AccessTestPanel(pkg) } }
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Built by Sonic team", fontWeight = FontWeight.SemiBold)
                        Text(
                            "j_120_x",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                ctx.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(IG_URL))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
