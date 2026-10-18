package com.example.calculator

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : Activity() {

    private lateinit var contentFrame: FrameLayout
    private lateinit var tabBasic: TextView
    private lateinit var tabTip: TextView
    private lateinit var tabBMI: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0D0F14")
        window.navigationBarColor = Color.parseColor("#0D0F14")

        // המסך הראשי
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0F14"))
            layoutDirection = View.LAYOUT_DIRECTION_RTL // כיוון ימין-לשמאל כללי
        }

        // --- יצירת סרגל הלשוניות (Tabs) העליון ---
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8f), dp(16f), dp(8f), dp(16f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#12151D")) }
        }

        tabBasic = createTab("🧮 רגיל") { switchTab(0) }
        tabTip = createTab("💰 טיפים") { switchTab(1) }
        tabBMI = createTab("⚖️ BMI") { switchTab(2) }

        val tabParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(4f), 0, dp(4f), 0)
        }

        tabBar.addView(tabBasic, tabParams)
        tabBar.addView(tabTip, tabParams)
        tabBar.addView(tabBMI, tabParams)

        // אזור התוכן המשתנה שבו יוצגו המחשבונים
        contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        root.addView(tabBar)
        root.addView(contentFrame)

        setContentView(root)
        
        // מפעיל את המחשבון הרגיל כברירת מחדל
        switchTab(0)
    }

    private fun createTab(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12f), 0, dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(24f).toFloat()
            }
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
    }

    private fun switchTab(index: Int) {
        val activeBg = Color.parseColor("#64B5F6")
        val inactiveBg = Color.parseColor("#1E2433")
        
        (tabBasic.background as GradientDrawable).setColor(if (index == 0) activeBg else inactiveBg)
        (tabTip.background as GradientDrawable).setColor(if (index == 1) activeBg else inactiveBg)
        (tabBMI.background as GradientDrawable).setColor(if (index == 2) activeBg else inactiveBg)
        
        tabBasic.setTextColor(if (index == 0) Color.BLACK else Color.parseColor("#8A94A8"))
        tabTip.setTextColor(if (index == 1) Color.BLACK else Color.parseColor("#8A94A8"))
        tabBMI.setTextColor(if (index == 2) Color.BLACK else Color.parseColor("#8A94A8"))

        contentFrame.removeAllViews()
        when (index) {
            0 -> contentFrame.addView(createBasicCalculator())
            1 -> contentFrame.addView(createTipCalculator())
            2 -> contentFrame.addView(createBMICalculator())
        }
    }

    // ==========================================
    // 1. מחשבון רגיל (מעוצב רספונסיבית לכל מסך)
    // ==========================================
    private var currentInput = "0"
    private var previousInput = ""
    private var operator: String? = null
    private var justCalculated = false
    private lateinit var expressionView: TextView
    private lateinit var currentView: TextView

    private fun createBasicCalculator(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR // המחשבון משמאל לימין כדי שהמספרים לא יתהפכו
            setPadding(dp(12f), dp(0f), dp(12f), dp(12f))
        }

        // אזור התצוגה העליון (35% מהמסך)
        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#181C26"), Color.parseColor("#1E2433"))
            ).apply { cornerRadius = dp(28f).toFloat() }
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            gravity = Gravity.BOTTOM or Gravity.END
        }

        expressionView = TextView(this).apply {
            text = if (operator != null) "$previousInput $operator" else ""
            setTextColor(Color.parseColor("#8A94A8"))
            setAutoSizeTextTypeUniformWithConfiguration(14, 24, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        currentView = TextView(this).apply {
            text = currentInput
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            // טקסט דינמי - קטן כשמקלידים המון מספרים כדי למנוע חיתוך!
            setAutoSizeTextTypeUniformWithConfiguration(20, 60, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f)
        }

        displayCard.addView(expressionView)
        displayCard.addView(currentView)
        
        // שימוש במשקל (Weight) כדי להתאים אוטומטית לכל טלפון
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.35f).apply { bottomMargin = dp(12f) })

        // לוח המקשים (65% מהמסך)
        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )

        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (symbol in rowSymbols) {
                val btn = createAnimatedButton(symbol)
                // התאמת הכפתורים בגודל שווה שמתמתח על כל המסך
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    val m = dp(5f)
                    setMargins(m, m, m, m)
                })
            }
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.65f))
        return root
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

        val shapeDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(24f).toFloat()
            setColor(defaultBgColor)
        }

        return TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            setAutoSizeTextTypeUniformWithConfiguration(16, 32, 2, TypedValue.COMPLEX_UNIT_SP)
            typeface = if (symbol == "=") Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shapeDrawable, null)
            isClickable = true

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        if (event.actionMasked == MotionEvent.ACTION_UP) onButtonClick(symbol)
                    }
                }
                false
            }
        }
    }

    private fun onButtonClick(symbol: String) {
        when (symbol) {
            "AC" -> { currentInput = "0"; previousInput = ""; operator = null; justCalculated = false }
            "⌫" -> { currentInput = if (currentInput.length > 1 && !justCalculated && currentInput != "Error") currentInput.dropLast(1) else "0"; if (currentInput == "-") currentInput = "0" }
            "±" -> { if (currentInput != "0" && currentInput != "Error") currentInput = if (currentInput.startsWith("-")) currentInput.substring(1) else "-$currentInput" }
            "+", "−", "×", "÷", "%" -> {
                if (currentInput == "Error") return
                if (previousInput.isNotEmpty() && operator != null && !justCalculated) calculate()
                previousInput = currentInput
                operator = symbol
                justCalculated = true
            }
            "=" -> { if (operator != null && previousInput.isNotEmpty()) calculate() }
            "." -> { if (justCalculated || currentInput == "Error") { currentInput = "0."; justCalculated = false } else if (!currentInput.contains(".")) currentInput += "." }
            else -> {
                if (justCalculated || currentInput == "0" || currentInput == "Error") { currentInput = symbol; justCalculated = false }
                else if (currentInput.length < 12) currentInput += symbol
            }
        }
        updateDisplay()
    }

    private fun calculate() {
        val prev = previousInput.toDoubleOrNull()
        val curr = currentInput.toDoubleOrNull()
        if (prev == null || curr == null || operator == null) return

        val res = when (operator) {
            "+" -> prev + curr
            "−" -> prev - curr
            "×" -> prev * curr
            "÷" -> if (curr == 0.0) Double.NaN else prev / curr
            "%" -> (prev / 100.0) * curr
            else -> return
        }

        currentInput = try {
            if (res.isNaN() || res.isInfinite()) "Error" else BigDecimal.valueOf(res).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        } catch (e: Exception) { "Error" }
        
        previousInput = ""
        operator = null
        justCalculated = true
    }

    private fun updateDisplay() {
        expressionView.text = if (operator != null) "$previousInput $operator" else ""
        currentView.text = currentInput
    }

    // ==========================================
    // רכיב קלט גנרי למחשבונים הנוספים
    // ==========================================
    private fun createInputBox(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(Color.parseColor("#6C7589"))
            setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            textSize = 18f
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#1E2433"))
                cornerRadius = dp(16f).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16f)
            }
        }
    }

    // ==========================================
    // 2. מחשבון טיפים
    // ==========================================
    private fun createTipCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val billInput = createInputBox("סכום החשבון (₪)")
        val tipInput = createInputBox("אחוז טיפ (%) - ברירת מחדל 10%")
        val peopleInput = createInputBox("מספר סועדים - ברירת מחדל 1")
        
        val resultView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            setLineSpacing(dp(8f).toFloat(), 1.2f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24f), 0, 0)
        }

        val calcBtn = TextView(this).apply {
            text = "חשב סכום"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0D0F14"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#64B5F6"))
                cornerRadius = dp(16f).toFloat()
            }
            setPadding(0, dp(16f), 0, dp(16f))
            isClickable = true
            
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val bill = billInput.text.toString().toDoubleOrNull()
                val tipPct = tipInput.text.toString().toDoubleOrNull() ?: 10.0
                val people = peopleInput.text.toString().toIntOrNull() ?: 1

                if (bill != null && people > 0) {
                    val tipAmount = bill * (tipPct / 100)
                    val total = bill + tipAmount
                    val perPerson = total / people
                    
                    resultView.text = "סה״כ טיפ: ${formatCurrency(tipAmount)} ₪\nסה״כ לתשלום: ${formatCurrency(total)} ₪\n\nלתשלום לכל סועד:\n${formatCurrency(perPerson)} ₪"
                } else {
                    resultView.text = "נא להזין סכום חשבון תקין."
                }
            }
        }

        root.addView(billInput)
        root.addView(tipInput)
        root.addView(peopleInput)
        root.addView(calcBtn)
        root.addView(resultView)
        scroll.addView(root)
        return scroll
    }

    private fun formatCurrency(value: Double): String {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    // ==========================================
    // 3. מחשבון BMI
    // ==========================================
    private fun createBMICalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val weightInput = createInputBox("משקל (ק״ג) - למשל 70")
        val heightInput = createInputBox("גובה (ס״מ) - למשל 175")
        
        val resultView = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            setLineSpacing(dp(6f).toFloat(), 1.1f)
            gravity = Gravity.CENTER
            setPadding(0, dp(24f), 0, 0)
        }

        val calcBtn = TextView(this).apply {
            text = "חשב מדד BMI"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#0D0F14"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#00E676"))
                cornerRadius = dp(16f).toFloat()
            }
            setPadding(0, dp(16f), 0, dp(16f))
            isClickable = true
            
            setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                val w = weightInput.text.toString().toDoubleOrNull()
                val hCm = heightInput.text.toString().toDoubleOrNull()
                
                if (w != null && hCm != null && hCm > 0) {
                    val hM = hCm / 100
                    val bmi = w / (hM * hM)
                    val status = when {
                        bmi < 18.5 -> "תת משקל 🔹"
                        bmi < 25 -> "משקל תקין ✅"
                        bmi < 30 -> "עודף משקל 🔸"
                        else -> "השמנת יתר ⚠️"
                    }
                    resultView.text = String.format("ה-BMI שלך: %.1f\n%s", bmi, status)
                } else {
                    resultView.text = "נא להזין נתונים תקינים."
                }
            }
        }

        root.addView(weightInput)
        root.addView(heightInput)
        root.addView(calcBtn)
        root.addView(resultView)
        scroll.addView(root)
        return scroll
    }

    private fun dp(value: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
    }
}
