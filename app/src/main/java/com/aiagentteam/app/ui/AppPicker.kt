package com.aiagentteam.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiagentteam.app.agents.AppItem

@Composable
fun AppPickerDialog(apps: List<AppItem>, onPick: (AppItem) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { it.label.contains(q, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("اختر التطبيق") },
        text = {
            Column {
                OutlinedTextField(q, { q = it }, label = { Text("بحث") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown) { a ->
                        Text(a.label, Modifier.fillMaxWidth()
                            .clickable { onPick(a) }.padding(vertical = 12.dp))
                    }
                }
            }
        }
    )
}
