package com.aiagentteam.app.agents

import android.content.Context
import android.content.Intent

data class AppItem(val label: String, val pkg: String)

object InstalledApps {
    fun list(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.pkg != ctx.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }
}
