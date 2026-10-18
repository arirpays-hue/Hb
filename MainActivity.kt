package com.example.calculator

import android.app.Activity
import android.app.DatePickerDialog
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
import java.util.Calendar

class MainActivity : Activity() {

    private lateinit var contentFrame: FrameLayout
    private lateinit var tabs: List<TextView>
    private lateinit var tabScroll: HorizontalScrollView
    private lateinit var titleText: TextView
    private lateinit var settingsBtn: TextView
    
    private var useHaptic = true
    private var calcPrecision = 8
    private var primaryColor = Color.parseColor("#64B5F6")
    private var currentTabIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#08090C")
        window.navigationBarColor = Color.parseColor("#08090C")

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
            layoutDirection = View.LAYOUT_DIRECTION_RTL 
        }

        val topHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(16f))
        }

        titleText = TextView(this).apply {
            text = "מחשבון פרימיום"
            setTextColor(Color.WHITE)
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        settingsBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#151822"))
                setStroke(dp(1.5f), Color.parseColor("#252A3A"))
            }
            setOnClickListener { performVibration(it); openSettings() }
        }

        topHeader.addView(titleText)
        topHeader.addView(settingsBtn)

        tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8f) }
        }

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16f), dp(4f), dp(16f), dp(12f))
        }

        tabs = listOf(
            createTab("🧮 רגיל", 0), createTab("📊 אחוזים", 1), createTab("🏷️ הנחות", 2),
            createTab("💰 טיפים", 3), createTab("⚖️ BMI", 4), createTab("🎂 גיל", 5)
        )

        val tabParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6f), 0, dp(6f), 0) }
        for (tab in tabs) tabBar.addView(tab, tabParams)
        tabScroll.addView(tabBar)

        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }

        root.addView(topHeader)
        root.addView(tabScroll)
        root.addView(contentFrame)

        setContentView(root)
        switchTab(0)
    }

    private fun performVibration(view: View) { if (useHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    private fun createTab(text: String, index: Int): TextView {
        return TextView(this).apply {
            this.text = text; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(20f), dp(10f), dp(20f), dp(10f))
            background = GradientDrawable().apply { cornerRadius = dp(24f).toFloat() }
            setOnClickListener { performVibration(it); switchTab(index) }
        }
    }

    private fun updateTabColors() {
        for (i in tabs.indices) {
            val bg = tabs[i].background as GradientDrawable
            if (i == currentTabIndex) {
                bg.setColor(primaryColor); bg.setStroke(0, Color.TRANSPARENT); tabs[i].setTextColor(Color.parseColor("#08090C"))
            } else {
                bg.setColor(Color.parseColor("#151822")); bg.setStroke(dp(1.5f), Color.parseColor("#252A3A")); tabs[i].setTextColor(Color.parseColor("#8A94A8"))
            }
        }
        val setBg = settingsBtn.background as GradientDrawable
        if (currentTabIndex == -1) { setBg.setColor(primaryColor); setBg.setStroke(0, Color.TRANSPARENT) } 
        else { setBg.setColor(Color.parseColor("#151822")); setBg.setStroke(dp(1.5f), Color.parseColor("#252A3A")) }
    }

    private fun openSettings() {
        currentTabIndex = -1; updateTabColors(); titleText.text = "הגדרות ועיצוב"
        tabScroll.visibility = View.GONE; contentFrame.removeAllViews(); contentFrame.addView(createSettingsPage())
    }

    private fun switchTab(index: Int) {
        currentTabIndex = index; tabScroll.visibility = View.VISIBLE
        titleText.text = when(index) { 0 -> "מחשבון פרימיום"; 1 -> "חישוב אחוזים"; 2 -> "חישוב הנחות"; 3 -> "חלוקת טיפים"; 4 -> "מדד BMI"; 5 -> "חישוב גיל"; else -> "מחשבון פרימיום" }
        updateTabColors(); contentFrame.removeAllViews()
        when (index) { 0 -> contentFrame.addView(createBasicCalculator()); 1 -> contentFrame.addView(createPercentageCalculator()); 2 -> contentFrame.addView(createDiscountCalculator()); 3 -> contentFrame.addView(createTipCalculator()); 4 -> contentFrame.addView(createBMICalculator()); 5 -> contentFrame.addView(createAgeCalculator()) }
    }
    private fun createInputBox(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText; setHintTextColor(Color.parseColor("#5C657A")); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; textSize = 18f
            setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#12151D")); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Color.parseColor("#252A3A")) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16f) }
        }
    }

    private fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView {
        val bgColor = if (isPrimary) primaryColor else Color.parseColor("#151822")
        val textColor = if (isPrimary) Color.parseColor("#08090C") else Color.WHITE
        val shape = GradientDrawable().apply { setColor(bgColor); cornerRadius = dp(18f).toFloat(); if (!isPrimary) setStroke(dp(1.5f), Color.parseColor("#252A3A")) }
        return TextView(this).apply {
            text = textStr; gravity = Gravity.CENTER; setTextColor(textColor); textSize = 20f; typeface = Typeface.DEFAULT_BOLD
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)
            setPadding(0, dp(18f), 0, dp(18f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }
            isClickable = true; setOnClickListener { performVibration(it); onClick(it) }
        }
    }

    private var currentInput = "0"
    private var previousInput = ""
    private var operator: String? = null
    private var justCalculated = false
    private lateinit var expressionView: TextView
    private lateinit var currentView: TextView

    private fun createBasicCalculator(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(16f), dp(0f), dp(16f), dp(16f)) }
        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(24f), dp(24f), dp(24f), dp(24f))
            background = GradientDrawable().apply { setColor(Color.parseColor("#12151D")); cornerRadius = dp(32f).toFloat(); setStroke(dp(1.5f), Color.parseColor("#252A3A")) }
        }
        expressionView = TextView(this).apply { text = if (operator != null) "$previousInput $operator" else ""; setTextColor(Color.parseColor("#8A94A8")); setAutoSizeTextTypeUniformWithConfiguration(16, 26, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        currentView = TextView(this).apply { text = currentInput; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setAutoSizeTextTypeUniformWithConfiguration(24, 64, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f) }
        displayCard.addView(expressionView); displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.35f).apply { bottomMargin = dp(16f) })

        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val rows = listOf(listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("±", "0", ".", "="))
        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (symbol in rowSymbols) rowLayout.addView(createAnimatedButton(symbol), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(6f), dp(6f), dp(6f), dp(6f)) })
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.65f))
        return root
    }

    private fun createAnimatedButton(symbol: String): TextView {
        val isOp = symbol in listOf("÷", "×", "−", "+")
        val defaultBg = when (symbol) { "AC" -> Color.parseColor("#2A171B"); "⌫", "%" -> Color.parseColor("#1A1F2B"); "÷", "×", "−", "+" -> Color.parseColor("#131B2A"); "=" -> primaryColor; else -> Color.parseColor("#151822") }
        val strokeColor = if (symbol == "=") Color.TRANSPARENT else Color.parseColor("#252A3A")
        val textColor = when (symbol) { "AC" -> Color.parseColor("#FF5252"); "⌫", "%", "÷", "×", "−", "+" -> primaryColor; "=" -> Color.parseColor("#08090C"); else -> Color.WHITE }
        val shape = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(24f).toFloat(); setColor(defaultBg); setStroke(dp(1.5f), strokeColor) }

        return TextView(this).apply {
            text = symbol; setTextColor(textColor); setAutoSizeTextTypeUniformWithConfiguration(18, 34, 2, TypedValue.COMPLEX_UNIT_SP); typeface = if (symbol == "=" || isOp) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL); gravity = Gravity.CENTER
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); isClickable = true
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { performVibration(v); v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).start() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(200).start(); if (e.actionMasked == MotionEvent.ACTION_UP) onButtonClick(symbol) }
                }
                false
            }
        }
    }

    private fun onButtonClick(s: String) {
        when (s) {
            "AC" -> { currentInput = "0"; previousInput = ""; operator = null; justCalculated = false }
            "⌫" -> { currentInput = if (currentInput.length > 1 && !justCalculated && currentInput != "Error") currentInput.dropLast(1) else "0"; if (currentInput == "-") currentInput = "0" }
            "±" -> { if (currentInput != "0" && currentInput != "Error") currentInput = if (currentInput.startsWith("-")) currentInput.substring(1) else "-$currentInput" }
            "+", "−", "×", "÷", "%" -> { if (currentInput == "Error") return; if (previousInput.isNotEmpty() && operator != null && !justCalculated) calculateBasic(); previousInput = currentInput; operator = s; justCalculated = true }
            "=" -> { if (operator != null && previousInput.isNotEmpty()) calculateBasic() }
            "." -> { if (justCalculated || currentInput == "Error") { currentInput = "0."; justCalculated = false } else if (!currentInput.contains(".")) currentInput += "." }
            else -> { if (justCalculated || currentInput == "0" || currentInput == "Error") { currentInput = s; justCalculated = false } else if (currentInput.length < 12) currentInput += s }
        }
        expressionView.text = if (operator != null) "$previousInput $operator" else ""; currentView.text = currentInput
    }

    private fun calculateBasic() {
        val prev = previousInput.toDoubleOrNull(); val curr = currentInput.toDoubleOrNull()
        if (prev == null || curr == null || operator == null) return
        val res = when (operator) { "+" -> prev + curr; "−" -> prev - curr; "×" -> prev * curr; "÷" -> if (curr == 0.0) Double.NaN else prev / curr; "%" -> (prev / 100.0) * curr; else -> return }
        currentInput = try { if (res.isNaN() || res.isInfinite()) "Error" else BigDecimal.valueOf(res).setScale(calcPrecision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() } catch (e: Exception) { "Error" }
        previousInput = ""; operator = null; justCalculated = true
    }
    private fun createPercentageCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val pctInput = createInputBox("אחוז (למשל 15)"); val totalInput = createInputBox("מתוך סכום (למשל 200)")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }
        val calcBtn = createActionBtn("חשב אחוז") {
            val pct = pctInput.text.toString().toDoubleOrNull(); val total = totalInput.text.toString().toDoubleOrNull()
            if (pct != null && total != null) resultView.text = "$pct% מתוך $total הם:\n\n${formatCurrency(total * (pct / 100.0))}" else resultView.text = "נא להזין נתונים תקינים."
        }
        root.addView(pctInput); root.addView(totalInput); root.addView(calcBtn); root.addView(resultView); scroll.addView(root); return scroll
    }

    private fun createDiscountCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val priceInput = createInputBox("מחיר מקורי (₪)"); val discountInput = createInputBox("אחוז הנחה (%)")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }
        val calcBtn = createActionBtn("חשב מחיר סופי") {
            val price = priceInput.text.toString().toDoubleOrNull(); val discount = discountInput.text.toString().toDoubleOrNull()
            if (price != null && discount != null && discount >= 0) { val saved = price * (discount / 100); resultView.text = "מחיר סופי: ₪${formatCurrency(price - saved)}\nחסכת: ₪${formatCurrency(saved)}" } else resultView.text = "נא להזין נתונים תקינים."
        }
        root.addView(priceInput); root.addView(discountInput); root.addView(calcBtn); root.addView(resultView); scroll.addView(root); return scroll
    }

    private fun createTipCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val billInput = createInputBox("סכום החשבון (₪)"); val tipInput = createInputBox("אחוז טיפ (%) - ברירת מחדל 10%"); val peopleInput = createInputBox("מספר סועדים - ברירת מחדל 1")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }
        val calcBtn = createActionBtn("חשב טיפ") {
            val bill = billInput.text.toString().toDoubleOrNull(); val tipPct = tipInput.text.toString().toDoubleOrNull() ?: 10.0; val people = peopleInput.text.toString().toIntOrNull() ?: 1
            if (bill != null && people > 0) { val tip = bill * (tipPct / 100); val total = bill + tip; resultView.text = "טיפ: ₪${formatCurrency(tip)}\nסה״כ: ₪${formatCurrency(total)}\n\nלכל סועד: ₪${formatCurrency(total / people)}" } else resultView.text = "נא להזין נתונים תקינים."
        }
        root.addView(billInput); root.addView(tipInput); root.addView(peopleInput); root.addView(calcBtn); root.addView(resultView); scroll.addView(root); return scroll
    }

    private fun createBMICalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val weightInput = createInputBox("משקל (ק״ג)"); val heightInput = createInputBox("גובה (ס״מ)")
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }
        val calcBtn = createActionBtn("חשב מדד BMI") {
            val w = weightInput.text.toString().toDoubleOrNull(); val h = heightInput.text.toString().toDoubleOrNull()
            if (w != null && h != null && h > 0) { val bmi = w / ((h / 100) * (h / 100)); val status = when { bmi < 18.5 -> "תת משקל 🔹"; bmi < 25 -> "משקל תקין ✅"; bmi < 30 -> "עודף משקל 🔸"; else -> "השמנת יתר ⚠️" }; resultView.text = String.format("ה-BMI שלך: %.1f\n%s", bmi, status) } else resultView.text = "נא להזין נתונים תקינים."
        }
        root.addView(weightInput); root.addView(heightInput); root.addView(calcBtn); root.addView(resultView); scroll.addView(root); return scroll
    }

    private fun createAgeCalculator(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val resultView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER; setPadding(0, dp(24f), 0, 0) }
        val btnPickDate = createActionBtn("בחר תאריך לידה 📅") {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val today = Calendar.getInstance()
                var ageYears = today.get(Calendar.YEAR) - year; var ageMonths = today.get(Calendar.MONTH) - month; var ageDays = today.get(Calendar.DAY_OF_MONTH) - day
                if (ageDays < 0) { ageMonths--; ageDays += today.getActualMaximum(Calendar.DAY_OF_MONTH) }
                if (ageMonths < 0) { ageYears--; ageMonths += 12 }
                if (ageYears < 0) resultView.text = "תאריך הלידה עתידי! נסה שוב." else resultView.text = "הגיל שלך הוא:\n\n$ageYears שנים\n$ageMonths חודשים\n$ageDays ימים\n\n🎉 עד מאה ועשרים!"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        root.addView(btnPickDate); root.addView(resultView); scroll.addView(root); return scroll
    }

    private fun createSettingsPage(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(0f), dp(20f), dp(20f)) }
        val titleColor = TextView(this).apply { text = "🎨 בחר צבע נושא (החלק לבחירה)"; setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)) }
        val colorScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(24f) } }
        val colorContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (hue in 0..350 step 15) {
            val c = Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.7f, 0.95f))
            val circle = View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c); if (c == primaryColor) setStroke(dp(3f), Color.WHITE) }
                layoutParams = LinearLayout.LayoutParams(dp(54f), dp(54f)).apply { setMargins(dp(6f), dp(6f), dp(6f), dp(6f)) }
                setOnClickListener { performVibration(it); primaryColor = c; openSettings() }
            }
            colorContainer.addView(circle)
        }
        colorScroll.addView(colorContainer)
        val titleSettings = TextView(this).apply { text = "הגדרות נוספות"; setTextColor(Color.WHITE); textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)) }
        val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", false) { useHaptic = !useHaptic; (it as TextView).text = if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴" }
        val precBtn = createActionBtn("דיוק מחשבון: $calcPrecision ספרות עשרוניות ⚙️", false) { calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2; (it as TextView).text = "דיוק מחשבון: $calcPrecision ספרות עשרוניות ⚙️" }
        val backBtn = createActionBtn("חזור למחשבון 🔙", true) { switchTab(0) }
        backBtn.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(24f) }
        root.addView(titleColor); root.addView(colorScroll); root.addView(titleSettings); root.addView(vibBtn); root.addView(precBtn); root.addView(backBtn)
        scroll.addView(root); return scroll
    }

    private fun formatCurrency(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    private fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
    
    
