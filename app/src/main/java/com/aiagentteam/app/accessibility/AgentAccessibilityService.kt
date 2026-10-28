package com.aiagentteam.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AgentAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var instance: AgentAccessibilityService? = null
    }

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun findEditable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isEditable) return n
        for (i in 0 until n.childCount) {
            val r = findEditable(n.getChild(i))
            if (r != null) return r
        }
        return null
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val field = findEditable(root) ?: return false
        val args = Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        return field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun clickByLabel(labels: List<String>): Boolean {
        val root = rootInActiveWindow ?: return false
        return clickMatch(root, labels.map { it.lowercase() })
    }

    private fun clickMatch(n: AccessibilityNodeInfo?, labels: List<String>): Boolean {
        if (n == null) return false
        val t = ((n.text?.toString() ?: "") + " " + (n.contentDescription?.toString() ?: "")).lowercase()
        if (labels.any { t.contains(it) }) {
            var c: AccessibilityNodeInfo? = n
            while (c != null && !c.isClickable) c = c.parent
            if (c != null && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        for (i in 0 until n.childCount) if (clickMatch(n.getChild(i), labels)) return true
        return false
    }

    fun dump(): String {
        val sb = StringBuilder()
        walk(rootInActiveWindow, sb)
        return sb.toString()
    }

    private fun walk(n: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (n == null || sb.length > 3000) return
        val t = n.text ?: n.contentDescription
        if (t != null) {
            sb.append(n.className?.toString()?.substringAfterLast('.') ?: "").append(": ").append(t).append("\n")
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), sb)
    }
}
