package com.example.calculator

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.service.quicksettings.TileService
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

class MiniCalcTileService : TileService() {
    
    private var currentInput = ""
    private var currentView: TextView? = null

    override fun onClick() {
        super.onClick()
        showMiniCalculator()
    }

    private fun showMiniCalculator() {
        // יצירת ממשק המיני מחשבון
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(16f), dp(16f), dp(16f))
            setBackgroundColor(Color.parseColor("#08090C"))
        }

        currentView = TextView(this).apply {
            text = "0"
            setTextColor(Color.WHITE)
            textSize = 32f
            gravity = Gravity.END
            setPadding(dp(16f), dp(16f), dp(16f), dp(24f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialogView.addView(currentView)

        val buttons = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=") // שורת הסיום
        )

        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (row in buttons) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (symbol in row) {
                val weight = if (symbol == "0") 2f else 1f
                val btn = createMiniButton(symbol).apply {
                    setOnClickListener { onBtnClick(symbol) }
                }
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply { setMargins(dp(4f), dp(4f), dp(4f), dp(4f)) })
            }
            keypad.addView(rowLayout)
        }
        dialogView.addView(keypad)

        showDialog(
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setView(dialogView)
                .create()
        )
    }

    private fun createMiniButton(symbol: String): TextView {
        val isOp = symbol in listOf("÷", "×", "−", "+", "=", "AC", "⌫", "%")
        val bgColorStr = when (symbol) {
            "AC" -> "#D32F2F"
            "=" -> "#64B5F6"
            in listOf("⌫", "%", "÷", "×", "−", "+") -> "#1A1D24"
            else -> "#141824"
        }
        val textColor = when {
            symbol == "AC" -> Color.WHITE
            symbol == "=" -> Color.parseColor("#08090C")
            isOp -> Color.parseColor("#64B5F6")
            else -> Color.WHITE
        }
        val shape = GradientDrawable().apply {
            cornerRadius = dp(16f).toFloat()
            setColor(Color.parseColor(bgColorStr))
        }
        return TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, dp(16f), 0, dp(16f))
            typeface = if (isOp) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            background = shape
        }
    }

    private fun onBtnClick(s: String) {
        when (s) {
            "AC" -> currentInput = ""
            "⌫" -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
            "=" -> {
                try {
                    val expr = currentInput.replace("×", "*").replace("÷", "/").replace("−", "-").replace("%", "/100")
                    val res = Utils.evalMathExpression(expr)
                    currentInput = Utils.formatResult(res, 8)
                } catch (e: Exception) { currentInput = "Error" }
            }
            else -> currentInput += s
        }
        currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
    }

    private fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
