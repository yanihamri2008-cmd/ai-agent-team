package com.aiagentteam.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.aiagentteam.app.agents.*
import com.aiagentteam.app.keyboard.SampleStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val ctx = LocalContext.current.applicationContext
    val orch = remember { Orchestrator(ctx) }
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf("Instagram") }
    var person by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var auto by remember { mutableStateOf(false) }
    var samples by remember { mutableStateOf(SampleStore.count(ctx)) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(topBar = { TopAppBar(title = { Text("فريق الذكاء الاصطناعي") }) }) { pad ->
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(app, { app = it }, label = { Text("التطبيق المستهدف") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(person, { person = it }, label = { Text("اسم الشخص") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(info, { info = it }, label = { Text("معلومات عن الشخص") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                item {
                    OutlinedTextField(instruction, { instruction = it }, label = { Text("ماذا تريد") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (auto) "الوضع التلقائي" else "وضع المراجعة", Modifier.weight(1f))
                        Switch(auto, { auto = it })
                    }
                }
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                orch.run(TaskInput(app, person, info, instruction,
                                    if (auto) RunMode.AUTO else RunMode.REVIEW))
                                samples = SampleStore.count(ctx)
                            }
                        },
                        enabled = !orch.running.value && instruction.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تشغيل الفريق") }
                }
                item { Text("عينات الكتابة المحفوظة: $samples", style = MaterialTheme.typography.titleMedium) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { samples = SampleStore.count(ctx) }) { Text("تحديث") }
                        OutlinedButton(onClick = { SampleStore.clear(ctx); samples = 0 }) { Text("مسح العينات") }
                    }
                }
                item { Text("الوكلاء", style = MaterialTheme.typography.titleMedium) }
                items(AgentRole.values().toList()) { role ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(role.title, Modifier.weight(1f))
                        Text(orch.statuses[role]?.label ?: "")
                    }
                }
                item { Text("سجل العمليات", style = MaterialTheme.typography.titleMedium) }
                items(orch.log.toList()) { e ->
                    Text("${e.role.title}: ${e.message}")
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
