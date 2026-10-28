package com.aiagentteam.app.keyboard

import android.content.Context
import org.json.JSONObject
import java.io.File

object SampleStore {
    private fun file(ctx: Context) = File(ctx.filesDir, "samples.jsonl")

    fun add(ctx: Context, text: String, gapsMs: List<Long>) {
        if (text.isBlank()) return
        val o = JSONObject()
        o.put("t", System.currentTimeMillis())
        o.put("text", text)
        o.put("gaps", gapsMs.joinToString(","))
        file(ctx).appendText(o.toString() + "\n")
    }

    fun count(ctx: Context): Int {
        val f = file(ctx)
        return if (f.exists()) f.readLines().size else 0
    }

    fun recent(ctx: Context, n: Int): List<String> {
        val f = file(ctx)
        if (!f.exists()) return emptyList()
        return f.readLines().takeLast(n).mapNotNull {
            runCatching { JSONObject(it).optString("text") }.getOrNull()?.takeIf { s -> s.isNotBlank() }
        }
    }

    fun clear(ctx: Context) { file(ctx).delete() }
}
