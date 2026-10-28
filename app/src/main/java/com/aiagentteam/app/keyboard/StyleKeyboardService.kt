package com.aiagentteam.app.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout

class StyleKeyboardService : InputMethodService() {
    private val buffer = StringBuilder()
    private val gaps = ArrayList<Long>()
    private var lastKey = 0L
    private var recording = true
    private var arabic = false

    private val latin = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val arabicRows = listOf("ضصثقفغعهخحجد", "شسيبلاتنمكط", "ئءؤرلاىةوزظ")

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val type = attribute?.inputType ?: 0
        val cls = type and InputType.TYPE_MASK_CLASS
        val variation = type and InputType.TYPE_MASK_VARIATION
        val isPassword = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        recording = !isPassword
        buffer.setLength(0)
        gaps.clear()
        lastKey = 0L
    }

    override fun onFinishInput() {
        flush()
        super.onFinishInput()
    }

    private fun flush() {
        if (recording && buffer.isNotBlank()) {
            SampleStore.add(this, buffer.toString(), gaps.toList())
        }
        buffer.setLength(0)
        gaps.clear()
        lastKey = 0L
    }

    private fun typed(s: String) {
        currentInputConnection?.commitText(s, 1)
        if (!recording) return
        val now = System.currentTimeMillis()
        if (lastKey != 0L) gaps.add(now - lastKey)
        lastKey = now
        buffer.append(s)
        if (s == "\n") flush()
    }

    private fun backspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        if (recording && buffer.isNotEmpty()) buffer.setLength(buffer.length - 1)
    }

    private fun send() {
        flush()
        currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_SEND)
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(4, 4, 4, 4)
        fun row(): LinearLayout {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER
            root.addView(r, LinearLayout.LayoutParams(-1, 0, 1f))
            return r
        }
        fun key(parent: LinearLayout, label: String, weight: Float = 1f, action: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.isAllCaps = false
            b.setPadding(0, 0, 0, 0)
            b.setOnClickListener { action() }
            parent.addView(b, LinearLayout.LayoutParams(0, -1, weight))
        }
        val rows = if (arabic) arabicRows else latin
        rows.forEach { chars ->
            val r = row()
            chars.forEach { c -> key(r, c.toString()) { typed(c.toString()) } }
        }
        val last = row()
        key(last, if (arabic) "EN" else "AR", 1.5f) {
            arabic = !arabic
            setInputView(onCreateInputView())
        }
        key(last, ",") { typed(",") }
        key(last, "space", 4f) { typed(" ") }
        key(last, ".") { typed(".") }
        key(last, "del", 1.5f) { backspace() }
        key(last, "send", 1.5f) { send() }
        root.minimumHeight = (resources.displayMetrics.density * 260).toInt()
        return root
    }
}
