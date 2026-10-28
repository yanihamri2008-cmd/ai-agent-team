package com.aiagentteam.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiagentteam.app.agents.Llm
import com.aiagentteam.app.agents.Orchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BrainPanel(orch: Orchestrator) {
    val ctx = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var has by remember { mutableStateOf(Llm.ready(ctx)) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                msg = withContext(Dispatchers.IO) {
                    runCatching {
                        ctx.contentResolver.openInputStream(uri)!!.use { i ->
                            Llm.modelFile(ctx).outputStream().use { o -> i.copyTo(o) }
                        }
                        Llm.reset()
                        "تم استيراد النموذج"
                    }.getOrElse { "فشل الاستيراد: ${it.message}" }
                }
                has = Llm.ready(ctx)
                busy = false
            }
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("النموذج والمسودة", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Text(if (has) "النموذج: جاهز (${Llm.modelFile(ctx).length()} بايت)" else "النموذج: غير موجود")
            OutlinedButton(
                onClick = { picker.launch(arrayOf("*/*")) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "جار النسخ، انتظر" else "استيراد ملف النموذج") }
            if (msg.isNotBlank()) Text(msg, fontSize = 13.sp)
            if (orch.draft.value.isNotBlank()) {
                OutlinedTextField(
                    value = orch.draft.value,
                    onValueChange = { orch.draft.value = it },
                    label = { Text("المسودة (يمكنك تعديلها)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = { scope.launch { orch.deliver() } },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("تنفيذ داخل التطبيق") }
            }
        }
    }
}
