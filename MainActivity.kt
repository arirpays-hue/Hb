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
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : Activity() {

    private lateinit var contentFrame: FrameLayout
    private lateinit var tabs: List<TextView>
    
    // הגדרות גלובליות של האפליקציה
    private var useHaptic = true
    private var calcPrecision = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#0D0F14")
        window.navigationBarColor = Color.parseColor("#0D0F14")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0F14"))
            layoutDirection = View.LAYOUT_DIRECTION_RTL // כיוון ימין-לשמאל כללי
        }

        // --- יצירת סרגל הלשוניות נגלל (HorizontalScrollView) ---
        val tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8f), dp(16f), dp(8f), dp(16f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#12151D")) }
        }

        // יצירת הלשוניות
        val tabBasic = createTab("🧮 רגיל") { switchTab(0) }
        val tabTip = createTab("💰 טיפים") { switchTab(1) }
        val tabBMI = createTab("⚖️ BMI") { switchTab(2) }
        val tabDiscount = createTab("🏷️ הנחות") { switchTab(3) }
        val tabSettings = createTab("⚙️ הגדרות") { switchTab(4) }

        tabs = listOf(tabBasic, tabTip, tabBMI, tabDiscount, tabSettings)

        val tabParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(6f), 0, dp(6f), 0)
        }

        for (tab in tabs) {
            tabBar.addView(tab, tabParams)
        }

        tabScroll.addView(tabBar)

        // אזור התוכן המשתנה
        contentFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        root.addView(tabScroll)
        root.addView(contentFrame)

        setContentView(root)
        
        switchTab(0) // התחלה במחשבון הרגיל
    }

    private fun performVibration(view: View) {
        if (useHaptic) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun createTab(text: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = dp(24f).toFloat()
            }
            setOnClickListener {
                performVibration(it)
                onClick()
            }
        }
    }

    private fun switchTab(index: Int) {
        val activeBg = Color.parseColor("#64B5F6")
        val inactiveBg = Color.parseColor("#1E2433")
        
        for (i in tabs.indices) {
            (tabs[i].background as GradientDrawable).setColor(if (i == index) activeBg else inactiveBg)
            tabs[i].setTextColor(if (i == index) Color.BLACK else Color.parseColor("#8A94A8"))
        }

        contentFrame.removeAllViews()
        when (index) {
            0 -> contentFrame.addView(createBasicCalculator())
            1 -> contentFrame.addView(createTipCalculator())
            2 -> contentFrame.addView(createBMICalculator())
            3 -> contentFrame.addView(createDiscountCalculator())
            4 -> contentFrame.addView(createSettingsPage())
        }
    }

    // ==========================================
    // רכיב קלט גנרי למסכים השונים
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
    // 1. מחשבון רגיל
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
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(12f), dp(0f), dp(12f), dp(12f))
        }

        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.parseColor("#181C26"), Color.parseColor("#1E2433"))).apply { cornerRadius = dp(28f).toFloat() }
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
            setAutoSizeTextTypeUniformWithConfiguration(20, 60, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f)
        }

        displayCard.addView(expressionView)
        displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.35f).apply { bottomMargin = dp(12f) })

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
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(5f), dp(5f), dp(5f), dp(5f)) })
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
                        performVibration(v)
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
                if (previousInput.isNotEmpty() && operator != null && !justCalculated) calculateBasic()
                previousInput = currentInput
                operator = symbol
                justCalculated = true
            }
            "=" -> { if (operator != null && previousInput.isNotEmpty()) calculateBasic() }
            "." -> { if (justCalculated || currentInput == "Error") { currentInput = "0."; justCalculated = false } else if (!currentInput.contains(".")) currentInput += "." }
            else -> {
                if (justCalculated || currentInput == "0" || currentInput == "Error") { currentInput = symbol; justCalculated = false }
                else if (currentInput.length < 12) currentInput += symbol
            }
        }
        expressionView.text = if (operator != null) "$previousInput $operator" else ""
        currentView.text = currentInput
    }

    private fun calculateBasic() {
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
            if (res.isNaN() || res.isInfinite()) "Error" 
            // שימוש בהגדרת הדיוק שהמשתמש בחר (calcPrecision)
            else BigDecimal.valueOf(res).setScale(calcPrecision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        } catch (e: Exception) { "Error" }
        
        previousInput = ""
        operator = null
        justCalculated = true
    }

    // ==========================================
    // 2. מחשבון טיפים
    // ==========================================
    private fun createTipCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24f), dp(24f), dp(24f), dp(24f)) }
        val billInput = createInputBox("סכום החשבון (₪)")
        val tipInput = createInputBox("אחוז טיפ (%) - ברירת מחדל 10%")
        val peopleInput = createInputBox("מספר סועדים - ברירת מחדל 1")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }

        val calcBtn = createActionBtn("חשב טיפ", "#64B5F6") {
            val bill = billInput.text.toString().toDoubleOrNull()
            val tipPct = tipInput.text.toString().toDoubleOrNull() ?: 10.0
            val people = peopleInput.text.toString().toIntOrNull() ?: 1
            if (bill != null && people > 0) {
                val tip = bill * (tipPct / 100); val total = bill + tip; val perPerson = total / people
                resultView.text = "סה״כ טיפ: ₪${formatCurrency(tip)}\nסה״כ לתשלום: ₪${formatCurrency(total)}\n\nלתשלום לכל סועד: ₪${formatCurrency(perPerson)}"
            } else resultView.text = "נא להזין נתונים תקינים."
        }

        root.addView(billInput); root.addView(tipInput); root.addView(peopleInput); root.addView(calcBtn); root.addView(resultView)
        scroll.addView(root)
        return scroll
    }

    // ==========================================
    // 3. מחשבון BMI
    // ==========================================
    private fun createBMICalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24f), dp(24f), dp(24f), dp(24f)) }
        val weightInput = createInputBox("משקל (ק״ג)")
        val heightInput = createInputBox("גובה (ס״מ)")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }

        val calcBtn = createActionBtn("חשב BMI", "#00E676") {
            val w = weightInput.text.toString().toDoubleOrNull()
            val h = heightInput.text.toString().toDoubleOrNull()
            if (w != null && h != null && h > 0) {
                val bmi = w / ((h / 100) * (h / 100))
                val status = when { bmi < 18.5 -> "תת משקל 🔹"; bmi < 25 -> "משקל תקין ✅"; bmi < 30 -> "עודף משקל 🔸"; else -> "השמנת יתר ⚠️" }
                resultView.text = String.format("ה-BMI שלך: %.1f\n%s", bmi, status)
            } else resultView.text = "נא להזין נתונים תקינים."
        }

        root.addView(weightInput); root.addView(heightInput); root.addView(calcBtn); root.addView(resultView)
        scroll.addView(root)
        return scroll
    }

    // ==========================================
    // 4. מחשבון הנחות (חדש)
    // ==========================================
    private fun createDiscountCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24f), dp(24f), dp(24f), dp(24f)) }
        val priceInput = createInputBox("מחיר מקורי (₪)")
        val discountInput = createInputBox("אחוז הנחה (%)")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }

        val calcBtn = createActionBtn("חשב מחיר סופי", "#FFB74D") {
            val price = priceInput.text.toString().toDoubleOrNull()
            val discount = discountInput.text.toString().toDoubleOrNull()
            if (price != null && discount != null && discount >= 0) {
                val saved = price * (discount / 100)
                val finalPrice = price - saved
                resultView.text = "מחיר סופי: ₪${formatCurrency(finalPrice)}\n\nחסכת: ₪${formatCurrency(saved)}"
            } else resultView.text = "נא להזין נתונים תקינים."
        }

        root.addView(priceInput); root.addView(discountInput); root.addView(calcBtn); root.addView(resultView)
        scroll.addView(root)
        return scroll
    }

    // ==========================================
    // 5. מסך הגדרות (חדש)
    // ==========================================
    private fun createSettingsPage(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24f), dp(24f), dp(24f), dp(24f)) }
        
        val title = TextView(this).apply {
            text = "הגדרות אפליקציה"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24f))
        }

        val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", "#262C3D", Color.WHITE) {
            useHaptic = !useHaptic
            (it as TextView).text = if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴"
        }

        val precBtn = createActionBtn("דיוק מחשבון רגיל: $calcPrecision ספרות ⚙️", "#262C3D", Color.WHITE) {
            calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2
            (it as TextView).text = "דיוק מחשבון רגיל: $calcPrecision ספרות ⚙️"
        }

        root.addView(title)
        root.addView(vibBtn)
        root.addView(precBtn)
        return root
    }

    // ==========================================
    // פונקציות עזר לבניית כפתורים ועיצוב
    // ==========================================
    private fun createActionBtn(textStr: String, bgColorStr: String, textColorInt: Int = Color.parseColor("#0D0F14"), onClick: (View) -> Unit): TextView {
        return TextView(this).apply {
            text = textStr
            gravity = Gravity.CENTER
            setTextColor(textColorInt)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply { setColor(Color.parseColor(bgColorStr)); cornerRadius = dp(16f).toFloat() }
            setPadding(0, dp(16f), 0, dp(16f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }
            isClickable = true
            setOnClickListener {
                performVibration(it)
                onClick(it)
            }
        }
    }

    private fun formatCurrency(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString()
    private fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
