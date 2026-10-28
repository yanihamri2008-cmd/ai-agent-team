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

    // 0 = latin, 1 = arabic, 2 = arabic alt
    private var layer = 0
    private var shift = false

    private fun pairs(s: String, upper: Boolean = false) = s.map {
        val c = if (upper) it.uppercaseChar().toString() else it.toString()
        c to c
    }

    private fun same(items: List<String>) = items.map { it to it }

    private val digits = "1234567890"
    private val latin = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val arabicMain = listOf("ضصثقفغعهخحج", "شسيبلاتنمكط", "ئءؤرىةوزظدذ")

    private val altRow1 = same(listOf("أ", "إ", "آ", "لا", "لأ", "لإ", "ـ"))
    private val altRow2 = listOf(
        "ـَ" to "\u064E", "ـُ" to "\u064F", "ـِ" to "\u0650", "ـّ" to "\u0651",
        "ـْ" to "\u0652", "ـً" to "\u064B", "ـٌ" to "\u064C", "ـٍ" to "\u064D"
    )
    private val altRow3 = same(listOf("،", "؟", "؛", "!", ":", "-", "(", ")", "/"))

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

    private fun rebuild() = setInputView(onCreateInputView())

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
        fun letters(items: List<Pair<String, String>>) {
            val r = row()
            items.forEach { (label, value) ->
                key(r, label) {
                    typed(value)
                    if (layer == 0 && shift) { shift = false; rebuild() }
                }
            }
        }

        letters(pairs(digits))
        when (layer) {
            0 -> latin.forEach { letters(pairs(it, shift)) }
            1 -> arabicMain.forEach { letters(pairs(it)) }
            else -> {
                letters(altRow1)
                letters(altRow2)
                letters(altRow3)
            }
        }

        val last = row()
        key(last, if (layer == 0) "AR" else "EN", 1.5f) {
            layer = if (layer == 0) 1 else 0
            shift = false
            rebuild()
        }
        when (layer) {
            0 -> key(last, "Aa", 1.2f) { shift = !shift; rebuild() }
            1 -> key(last, "alt", 1.2f) { layer = 2; rebuild() }
            else -> key(last, "main", 1.2f) { layer = 1; rebuild() }
        }
        key(last, if (layer == 0) "," else "،") { typed(if (layer == 0) "," else "،") }
        key(last, "space", 3.5f) { typed(" ") }
        key(last, ".") { typed(".") }
        key(last, "del", 1.3f) { backspace() }
        key(last, "send", 1.5f) { send() }

        root.minimumHeight = (resources.displayMetrics.density * 290).toInt()
        return root
    }
}
