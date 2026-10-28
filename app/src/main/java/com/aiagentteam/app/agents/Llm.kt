package com.aiagentteam.app.agents

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object Llm {
    private var engine: Engine? = null
    private val lock = Mutex()

    fun modelFile(ctx: Context) = File(ctx.filesDir, "model.litertlm")
    fun ready(ctx: Context) = modelFile(ctx).let { it.exists() && it.length() > 1_000_000 }

    fun reset() {
        runCatching { engine?.close() }
        engine = null
    }

    suspend fun ask(ctx: Context, prompt: String): String = lock.withLock {
        withContext(Dispatchers.Default) {
            val e = engine ?: Engine(
                EngineConfig(
                    modelPath = modelFile(ctx).absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = ctx.cacheDir.path
                )
            ).also { it.initialize(); engine = it }
            e.createConversation().use { c ->
                val sb = StringBuilder()
                c.sendMessageAsync(prompt).collect { sb.append(it.toString()) }
                sb.toString().trim()
            }
        }
    }
}
