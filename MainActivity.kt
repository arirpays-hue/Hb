package com.example.calculator

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : Activity() {

    private var currentInput = "0"
    private var previousInput = ""
    private var operator: String? = null
    private var justCalculated = false
    private var showHistory = false
    private val historyList = mutableListOf<Pair<String, String>>()

    private lateinit var expressionView: TextView
    private lateinit var currentView: TextView
    private lateinit var historyToggleBtn: TextView
    private lateinit var clearHistoryBtn: TextView
    private lateinit var historyScroll: ScrollView
    private lateinit var historyContainer: LinearLayout
    private lateinit var displayCard: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#0D0F14")
        window.navigationBarColor = Color.parseColor("#0D0F14")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0F14"))
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            val pad = dp(14f)
            setPadding(pad, pad, pad, pad)
        }

        // --- כרטיס תצוגה עליון (35% מגובה המסך) ---
        displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#181C26"), Color.parseColor("#1E2433"))
            ).apply {
                cornerRadius = dp(28f).toFloat()
            }
            val pad = dp(16f)
            setPadding(pad, pad, pad, pad)
        }

        // שורה עליונה - כפתור היסטוריה
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        historyToggleBtn = TextView(this).apply {
            text = "🕒 היסטוריה (0)"
            setTextColor(Color.parseColor("#B0B8C8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#262C3D"))
                cornerRadius = dp(50f).toFloat()
            }
            setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                toggleHistory()
            }
        }

        val topSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }

        clearHistoryBtn = TextView(this).apply {
            text = "נקה הכל"
            setTextColor(Color.parseColor("#FF6B6B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            visibility = View.GONE
            setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                historyList.clear()
                refreshHistoryUI()
            }
        }

        topBar.addView(historyToggleBtn)
        topBar.addView(topSpacer)
        topBar.addView(clearHistoryBtn)
        displayCard.addView(topBar)

        // חלל גמיש באמצע הכרטיס (מכיל את ההיסטוריה כשפותחים אותה)
        val middleBox = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        historyScroll = ScrollView(this).apply {
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = dp(8f)
                bottomMargin = dp(4f)
            }
        }

        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        historyScroll.addView(historyContainer)
        middleBox.addView(historyScroll)
        displayCard.addView(middleBox)

        // אזור המספרים והתוצאה - תמיד מיושר לתחתית הכרטיס
        expressionView = TextView(this).apply {
            text = ""
            setTextColor(Color.parseColor("#8A94A8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.END
            setSingleLine(true)
        }

        currentView = TextView(this).apply {
            text = "0"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 48f)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            gravity = Gravity.END
            setSingleLine(true)
        }

        displayCard.addView(expressionView)
        displayCard.addView(currentView)

        root.addView(
            displayCard,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.35f).apply {
                bottomMargin = dp(12f)
            }
        )

        // --- לוח מקשים (65% מגובה המסך, מחולק שווה בשווה ל-5 שורות) ---
        val keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            for (symbol in rowSymbols) {
                val btn = createAnimatedButton(symbol)
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    val m = dp(5f)
                    setMargins(m, m, m, m)
                }
                rowLayout.addView(btn, lp)
            }
            keypad.addView(
                rowLayout,
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            )
        }

        root.addView(
            keypad,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.65f)
        )

        setContentView(root)
    }

    private fun createAnimatedButton(symbol: String): TextView {
        val defaultBgColor = when (symbol) {
            "AC" -> Color.parseColor("#422026")
            "⌫", "%" -> Color.parseColor("#283042")
            "÷", "×", "−", "+" -> Color.parseColor("#1D3557")
            "=" -> Color.parseColor("#00E676")
            else -> Color.parseColor("#1B1F2B")
        }

        val textColor = when (symbol) {
            "AC" -> Color.parseColor("#FF6B6B")
            "⌫", "%" -> Color.parseColor("#00E676")
            "÷", "×", "−", "+" -> Color.parseColor("#82B1FF")
            "=" -> Color.parseColor("#0D0F14")
            else -> Color.WHITE
        }

        val normalRadius = dp(36f).toFloat()
        val pressedRadius = dp(14f).toFloat()

        val shapeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = normalRadius
            setColor(defaultBgColor)
        }

        val rippleColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
        val rippleDrawable = RippleDrawable(rippleColor, shapeDrawable, null)

        return TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                if (symbol in listOf("÷", "×", "−", "+", "=")) 28f else 24f
            )
            typeface = if (symbol == "=") {
                Typeface.DEFAULT_BOLD
            } else {
                Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            gravity = Gravity.CENTER
            background = rippleDrawable
            isClickable = true

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        v.animate()
                            .scaleX(0.88f)
                            .scaleY(0.88f)
                            .setDuration(90)
                            .setInterpolator(DecelerateInterpolator())
                            .start()

                        ValueAnimator.ofFloat(shapeDrawable.cornerRadius, pressedRadius).apply {
                            duration = 90
                            addUpdateListener { shapeDrawable.cornerRadius = it.animatedValue as Float }
                            start()
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240)
                            .setInterpolator(OvershootInterpolator(2.6f))
                            .start()

                        ValueAnimator.ofFloat(shapeDrawable.cornerRadius, normalRadius).apply {
                            duration = 200
                            addUpdateListener { shapeDrawable.cornerRadius = it.animatedValue as Float }
                            start()
                        }

                        if (event.actionMasked == MotionEvent.ACTION_UP) {
                            onButtonClick(symbol)
                        }
                    }
                }
                false
            }
        }
    }

    private fun onButtonClick(symbol: String) {
        when (symbol) {
            "AC" -> {
                currentInput = "0"
                previousInput = ""
                operator = null
                justCalculated = false
                animateDisplayChange(false)
            }
            "⌫" -> {
                if (justCalculated || currentInput == "Error") {
                    currentInput = "0"
                    justCalculated = false
                } else {
                    currentInput = if (currentInput.length > 1) currentInput.dropLast(1) else "0"
                    if (currentInput == "-") currentInput = "0"
                }
                updateDisplay()
            }
            "±" -> {
                if (currentInput != "0" && currentInput != "Error") {
                    currentInput = if (currentInput.startsWith("-")) {
                        currentInput.substring(1)
                    } else {
                        "-$currentInput"
                    }
                    updateDisplay()
                }
            }
            "+", "−", "×", "÷", "%" -> {
                if (currentInput == "Error") return
                if (previousInput.isNotEmpty() && operator != null && !justCalculated) {
                    calculate(animate = false)
                }
                previousInput = currentInput
                operator = symbol
                justCalculated = true
                updateDisplay()
            }
            "=" -> {
                if (operator != null && previousInput.isNotEmpty()) {
                    calculate(animate = true)
                }
            }
            "." -> {
                if (justCalculated || currentInput == "Error") {
                    currentInput = "0."
                    justCalculated = false
                } else if (!currentInput.contains(".")) {
                    currentInput += "."
                }
                updateDisplay()
            }
            else -> { // ספרות 0-9
                if (justCalculated || currentInput == "0" || currentInput == "Error") {
                    currentInput = symbol
                    justCalculated = false
                } else if (currentInput.length < 12) {
                    currentInput += symbol
                }
                updateDisplay()
            }
        }
    }

    private fun calculate(animate: Boolean) {
        val prev = previousInput.toDoubleOrNull()
        val curr = currentInput.toDoubleOrNull()
        val op = operator
        if (prev == null || curr == null || op == null) return

        val res = when (op) {
            "+" -> prev + curr
            "−" -> prev - curr
            "×" -> prev * curr
            "÷" -> if (curr == 0.0) Double.NaN else prev / curr
            "%" -> (prev / 100.0) * curr
            else -> return
        }

        val formatted = formatResult(res)
        val expr = "$previousInput $op $currentInput"
        if (formatted != "Error") {
            historyList.add(0, expr to formatted)
            refreshHistoryUI()
        }

        expressionView.text = "$expr ="
        currentInput = formatted
        previousInput = ""
        operator = null
        justCalculated = true

        if (animate) {
            animateDisplayChange(true)
        } else {
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        if (operator != null) {
            expressionView.text = "$previousInput $operator"
        } else if (!justCalculated) {
            expressionView.text = ""
        }

        currentView.text = currentInput
        val textSize = when {
            currentInput.length > 10 -> 30f
            currentInput.length > 7 -> 38f
            else -> 48f
        }
        currentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }

    private fun animateDisplayChange(isResult: Boolean) {
        updateDisplay()
        currentView.translationY = if (isResult) dp(18f).toFloat() else -dp(10f).toFloat()
        currentView.alpha = 0.2f
        currentView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .setInterpolator(OvershootInterpolator(1.8f))
            .start()
    }

    private fun toggleHistory() {
        showHistory = !showHistory
        TransitionManager.beginDelayedTransition(displayCard, AutoTransition().setDuration(180))
        historyScroll.visibility = if (showHistory) View.VISIBLE else View.GONE
        refreshHistoryUI()
    }

    private fun refreshHistoryUI() {
        historyToggleBtn.text = if (showHistory) "✕ סגור" else "🕒 היסטוריה (${historyList.size})"
        clearHistoryBtn.visibility = if (showHistory && historyList.isNotEmpty()) View.VISIBLE else View.GONE

        historyContainer.removeAllViews()
        if (historyList.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "אין חישובים קודמים"
                setTextColor(Color.parseColor("#6C7589"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER
                setPadding(0, dp(12f), 0, dp(12f))
            }
            historyContainer.addView(emptyTv)
            return
        }

        for (item in historyList) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#141821"))
                    cornerRadius = dp(10f).toFloat()
                }
                setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(6f) }

                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    currentInput = item.second
                    justCalculated = true
                    toggleHistory()
                    animateDisplayChange(true)
                }
            }

            val exprTv = TextView(this).apply {
                text = item.first
                setTextColor(Color.parseColor("#9EA7B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val resTv = TextView(this).apply {
                text = "= ${item.second}"
                setTextColor(Color.parseColor("#00E676"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
            }

            row.addView(exprTv)
            row.addView(resTv)
            historyContainer.addView(row)
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return try {
            BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            resources.displayMetrics
        ).toInt()
    }
}
