package com.aiagentteam.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiagentteam.app.agents.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val orch = remember { Orchestrator() }
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf("Instagram") }
    var person by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var auto by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("AI Agent Team") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(app, { app = it }, label = { Text("Target app") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(person, { person = it }, label = { Text("Person name") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(info, { info = it }, label = { Text("Info about the person") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            item {
                OutlinedTextField(instruction, { instruction = it }, label = { Text("What do you want") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
            item {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (auto) "Auto Mode" else "Review Mode", Modifier.weight(1f))
                    Switch(auto, { auto = it })
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            orch.run(TaskInput(app, person, info, instruction,
                                if (auto) RunMode.AUTO else RunMode.REVIEW))
                        }
                    },
                    enabled = !orch.running.value && instruction.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Run team") }
            }
            item { Text("Agents", style = MaterialTheme.typography.titleMedium) }
            items(AgentRole.values().toList()) { role ->
                Row(Modifier.fillMaxWidth()) {
                    Text(role.title, Modifier.weight(1f))
                    Text(orch.statuses[role]?.label ?: "")
                }
            }
            item { Text("Log", style = MaterialTheme.typography.titleMedium) }
            items(orch.log.toList()) { e ->
                Text("${e.role.title}: ${e.message}")
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
