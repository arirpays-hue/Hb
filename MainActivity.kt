package com.example.calculator

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import kotlin.math.pow

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootLayout: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var tabs: List<TextView>
    private lateinit var tabScroll: HorizontalScrollView
    private lateinit var titleText: TextView
    private lateinit var settingsBtn: TextView
    private lateinit var historyBtn: TextView

    // הגדרות שנשמרות בזיכרון המכשיר
    private var useHaptic = true
    private var useSound = true
    private var keepScreenOn = false
    private var calcPrecision = 8
    private var buttonCornerStyle = 24f // 8=מרובע, 24=מודרני, 40=עגול
    private var primaryColor = Color.parseColor("#64B5F6")
    private var bgColor = Color.parseColor("#08090C")
    private var memoryValue = 0.0
    private var currentTabIndex = 0
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("CalcProPrefs", Context.MODE_PRIVATE)
        loadPreferences()

        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val topHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18f), dp(18f), dp(18f), dp(12f))
        }

        titleText = TextView(this).apply {
            text = "מחשבון פרימיום"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        historyBtn = TextView(this).apply {
            text = "🕒"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(dp(11f), dp(11f), dp(11f), dp(11f))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(8f) }
            setOnClickListener { feedback(it); showHistoryDialog() }
        }

        settingsBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(dp(11f), dp(11f), dp(11f), dp(11f))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            setOnClickListener { feedback(it); openSettings() }
        }

        topHeader.addView(titleText)
        topHeader.addView(historyBtn)
        topHeader.addView(settingsBtn)

        tabScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(6f) }
        }

        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14f), dp(4f), dp(14f), dp(10f))
        }

        tabs = listOf(
            createTab("🧮 רגיל", 0), createTab("🔬 מדעי", 1), createTab("🧾 מע״מ", 2),
            createTab("🏦 הלוואה", 3), createTab("📏 מרחק", 4), createTab("⚖️ משקל", 5),
            createTab("🌡️ טמפ׳", 6), createTab("💻 מתכנתים", 7), createTab("📊 אחוזים", 8),
            createTab("🏷️ הנחות", 9), createTab("💰 טיפים", 10), createTab("🏃 BMI", 11), createTab("🎂 גיל", 12)
        )

        val tabParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(5f), 0, dp(5f), 0) }
        for (tab in tabs) tabBar.addView(tab, tabParams)
        tabScroll.addView(tabBar)

        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }

        rootLayout.addView(topHeader)
        rootLayout.addView(tabScroll)
        rootLayout.addView(contentFrame)

        setContentView(rootLayout)
        applyTheme()
    }

    private fun loadPreferences() {
        useHaptic = prefs.getBoolean("useHaptic", true)
        useSound = prefs.getBoolean("useSound", true)
        keepScreenOn = prefs.getBoolean("keepScreenOn", false)
        calcPrecision = prefs.getInt("calcPrecision", 8)
        buttonCornerStyle = prefs.getFloat("buttonCornerStyle", 24f)
        primaryColor = prefs.getInt("primaryColor", Color.parseColor("#64B5F6"))
        bgColor = prefs.getInt("bgColor", Color.parseColor("#08090C"))
        memoryValue = Double.fromBits(prefs.getLong("memoryValue", 0L))
        val savedHist = prefs.getString("history", "") ?: ""
        historyList.clear()
        if (savedHist.isNotEmpty()) historyList.addAll(savedHist.split("||"))
        updateKeepScreenOn()
    }

    private fun savePreferences() {
        prefs.edit()
            .putBoolean("useHaptic", useHaptic)
            .putBoolean("useSound", useSound)
            .putBoolean("keepScreenOn", keepScreenOn)
            .putInt("calcPrecision", calcPrecision)
            .putFloat("buttonCornerStyle", buttonCornerStyle)
            .putInt("primaryColor", primaryColor)
            .putInt("bgColor", bgColor)
            .putLong("memoryValue", memoryValue.toRawBits())
            .putString("history", historyList.take(30).joinToString("||"))
            .apply()
    }

    private fun updateKeepScreenOn() {
        if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
        private fun applyTheme() {
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        rootLayout.setBackgroundColor(bgColor)
        updateTabColors()
        if (currentTabIndex == -1) {
            contentFrame.removeAllViews()
            contentFrame.addView(createSettingsPage())
        } else {
            switchTab(currentTabIndex)
        }
    }

    private fun getCardColor(): Int = lightenColor(bgColor, 0.06f)
    private fun getStrokeColor(): Int = lightenColor(bgColor, 0.15f)

    private fun lightenColor(color: Int, fraction: Float): Int {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return Color.rgb((r + (255 - r) * fraction).toInt(), (g + (255 - g) * fraction).toInt(), (b + (255 - b) * fraction).toInt())
    }

    private fun feedback(view: View) {
        if (useHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (useSound) view.playSoundEffect(SoundEffectConstants.CLICK)
    }

    private fun copyToClipboard(text: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("CalcResult", text))
        Toast.makeText(this, "הועתק ללוח: $text 📋", Toast.LENGTH_SHORT).show()
    }

    private fun showHistoryDialog() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "היסטוריית החישובים ריקה כרגע", Toast.LENGTH_SHORT).show()
            return
        }
        val items = historyList.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("🕒 היסטוריית חישובים (לחץ להזנה)")
            .setItems(items) { _, which ->
                val selected = items[which].substringAfter("=").trim()
                currentInput = selected
                justCalculated = true
                if (currentTabIndex != 0 && currentTabIndex != 1) switchTab(0)
                else currentView?.text = currentInput
            }
            .setNeutralButton("נקה הכל") { _, _ ->
                historyList.clear()
                savePreferences()
                Toast.makeText(this, "ההיסטוריה נמחקה", Toast.LENGTH_SHORT).show()
            }
            .setPositiveButton("סגור", null)
            .show()
    }

    private fun createTab(text: String, index: Int): TextView {
        return TextView(this).apply {
            this.text = text; gravity = Gravity.CENTER; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(16f), dp(9f), dp(16f), dp(9f))
            background = GradientDrawable().apply { cornerRadius = dp(24f).toFloat() }
            setOnClickListener { feedback(it); switchTab(index) }
        }
    }

    private fun updateTabColors() {
        for (i in tabs.indices) {
            val bg = tabs[i].background as GradientDrawable
            if (i == currentTabIndex) {
                bg.setColor(primaryColor); bg.setStroke(0, Color.TRANSPARENT); tabs[i].setTextColor(Color.parseColor("#08090C"))
            } else {
                bg.setColor(getCardColor()); bg.setStroke(dp(1.5f), getStrokeColor()); tabs[i].setTextColor(Color.parseColor("#8A94A8"))
            }
        }
        val setBg = settingsBtn.background as GradientDrawable
        if (currentTabIndex == -1) { setBg.setColor(primaryColor); setBg.setStroke(0, Color.TRANSPARENT) }
        else { setBg.setColor(getCardColor()); setBg.setStroke(dp(1.5f), getStrokeColor()) }

        val histBg = historyBtn.background as GradientDrawable
        histBg.setColor(getCardColor()); histBg.setStroke(dp(1.5f), getStrokeColor())
    }

    private fun openSettings() {
        currentTabIndex = -1; updateTabColors(); titleText.text = "הגדרות ועיצוב"
        tabScroll.visibility = View.GONE; contentFrame.removeAllViews(); contentFrame.addView(createSettingsPage())
    }

    private fun switchTab(index: Int) {
        currentTabIndex = index; tabScroll.visibility = View.VISIBLE
        titleText.text = when(index) {
            0 -> "מחשבון רגיל"; 1 -> "מחשבון מדעי"; 2 -> "מחשבון מע״מ"; 3 -> "מחשבון הלוואות"
            4 -> "המרת מרחקים"; 5 -> "המרת משקלים"; 6 -> "המרת טמפרטורה"; 7 -> "מחשבון מתכנתים"
            8 -> "חישוב אחוזים"; 9 -> "חישוב הנחות"; 10 -> "חלוקת טיפים"; 11 -> "מדד BMI"; 12 -> "חישוב גיל"; else -> ""
        }
        updateTabColors(); contentFrame.removeAllViews()
        val view = when (index) {
            0 -> createBasicCalculator(); 1 -> createScientificCalculator(); 2 -> createVatCalculator()
            3 -> createLoanCalculator(); 4 -> createDistanceCalculator(); 5 -> createWeightCalculator()
            6 -> createTempCalculator(); 7 -> createProgrammerCalculator(); 8 -> createPercentageCalculator()
            9 -> createDiscountCalculator(); 10 -> createTipCalculator(); 11 -> createBMICalculator(); 12 -> createAgeCalculator()
            else -> createBasicCalculator()
        }
        contentFrame.addView(view)
    }

    private var currentInput = "0"
    private var previousInput = ""
    private var operator: String? = null
    private var justCalculated = false
    private var expressionView: TextView? = null
    private var currentView: TextView? = null
    private var memoryIndicator: TextView? = null

    private fun createBasicCalculator(): View = createCalculatorLayout(listOf(
        listOf("MC", "MR", "M+", "M-"),
        listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("±", "0", ".", "=")
    ))

    private fun createScientificCalculator(): View = createCalculatorLayout(listOf(
        listOf("MC", "MR", "M+", "M-"),
        listOf("sin", "cos", "tan", "π"), listOf("√", "x²", "x³", "e"),
        listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("±", "0", ".", "=")
    ))
        private fun createCalculatorLayout(rows: List<List<String>>): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(14f), dp(0f), dp(14f), dp(14f)) }
        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(18f), dp(14f), dp(18f), dp(14f))
            background = GradientDrawable().apply { setColor(getCardColor()); cornerRadius = dp(28f).toFloat(); setStroke(dp(1.5f), getStrokeColor()) }
            setOnClickListener { feedback(it); copyToClipboard(currentInput) }
        }
        memoryIndicator = TextView(this).apply {
            text = if (memoryValue != 0.0) "M = ${formatResult(memoryValue)}   (לחץ על התוצאה להעתקה 📋)" else "לחץ על התוצאה להעתקה 📋"
            setTextColor(primaryColor); textSize = 11f; gravity = Gravity.START
        }
        expressionView = TextView(this).apply { text = if (operator != null) "$previousInput $operator" else ""; setTextColor(Color.parseColor("#8A94A8")); setAutoSizeTextTypeUniformWithConfiguration(12, 22, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        currentView = TextView(this).apply { text = currentInput; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setAutoSizeTextTypeUniformWithConfiguration(22, 56, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f) }
        displayCard.addView(memoryIndicator); displayCard.addView(expressionView); displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.32f).apply { bottomMargin = dp(12f) })

        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (symbol in rowSymbols) rowLayout.addView(createAnimatedButton(symbol), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(4f), dp(4f), dp(4f), dp(4f)) })
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.68f))
        return root
    }

    private fun createAnimatedButton(symbol: String): TextView {
        val isMem = symbol in listOf("MC", "MR", "M+", "M-")
        val isOp = symbol in listOf("÷", "×", "−", "+", "sin", "cos", "tan", "√", "x²", "x³", "π", "e")
        val defaultBg = when {
            symbol == "AC" -> lightenColor(Color.RED, 0.2f)
            symbol == "=" -> primaryColor
            isMem -> lightenColor(bgColor, 0.03f)
            symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> lightenColor(bgColor, 0.1f)
            else -> getCardColor()
        }
        val strokeColor = if (symbol == "=" || symbol == "AC") Color.TRANSPARENT else getStrokeColor()
        val textColor = when {
            symbol == "AC" -> Color.WHITE
            symbol == "=" -> Color.parseColor("#08090C")
            isMem || symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> primaryColor
            else -> Color.WHITE
        }
        val shape = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(buttonCornerStyle).toFloat(); setColor(defaultBg); setStroke(dp(1.5f), strokeColor) }

        return TextView(this).apply {
            text = symbol; setTextColor(textColor); setAutoSizeTextTypeUniformWithConfiguration(12, 28, 2, TypedValue.COMPLEX_UNIT_SP); typeface = if (symbol == "=" || isOp || isMem) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL); gravity = Gravity.CENTER
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); isClickable = true
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { feedback(v); v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(90).start() }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(180).start(); if (e.actionMasked == MotionEvent.ACTION_UP) onButtonClick(symbol) }
                }
                false
            }
        }
    }

    private fun onButtonClick(s: String) {
        try {
            when (s) {
                "MC" -> { memoryValue = 0.0; savePreferences() }
                "MR" -> { currentInput = formatResult(memoryValue); justCalculated = true }
                "M+" -> { memoryValue += (currentInput.toDoubleOrNull() ?: 0.0); savePreferences(); justCalculated = true }
                "M-" -> { memoryValue -= (currentInput.toDoubleOrNull() ?: 0.0); savePreferences(); justCalculated = true }
                "AC" -> { currentInput = "0"; previousInput = ""; operator = null; justCalculated = false }
                "⌫" -> { currentInput = if (currentInput.length > 1 && !justCalculated && currentInput != "Error") currentInput.dropLast(1) else "0"; if (currentInput == "-") currentInput = "0" }
                "±" -> { if (currentInput != "0" && currentInput != "Error") currentInput = if (currentInput.startsWith("-")) currentInput.substring(1) else "-$currentInput" }
                "π" -> { currentInput = formatResult(Math.PI); justCalculated = true }
                "e" -> { currentInput = formatResult(Math.E); justCalculated = true }
                "sin", "cos", "tan" -> {
                    val rad = Math.toRadians(currentInput.toDouble())
                    val res = when(s) { "sin" -> Math.sin(rad); "cos" -> Math.cos(rad); else -> Math.tan(rad) }
                    currentInput = formatResult(res); justCalculated = true
                }
                "√" -> { currentInput = formatResult(Math.sqrt(currentInput.toDouble())); justCalculated = true }
                "x²" -> { val v = currentInput.toDouble(); currentInput = formatResult(v * v); justCalculated = true }
                "x³" -> { val v = currentInput.toDouble(); currentInput = formatResult(v * v * v); justCalculated = true }
                "+", "−", "×", "÷", "%" -> { if (currentInput == "Error") return; if (previousInput.isNotEmpty() && operator != null && !justCalculated) calculateBasic(); previousInput = currentInput; operator = s; justCalculated = true }
                "=" -> { if (operator != null && previousInput.isNotEmpty()) calculateBasic() }
                "." -> { if (justCalculated || currentInput == "Error") { currentInput = "0."; justCalculated = false } else if (!currentInput.contains(".")) currentInput += "." }
                else -> { if (justCalculated || currentInput == "0" || currentInput == "Error") { currentInput = s; justCalculated = false } else if (currentInput.length < 15) currentInput += s }
            }
        } catch (e: Exception) { currentInput = "Error" }
        memoryIndicator?.text = if (memoryValue != 0.0) "M = ${formatResult(memoryValue)}   (לחץ להעתקה 📋)" else "לחץ על התוצאה להעתקה 📋"
        expressionView?.text = if (operator != null) "$previousInput $operator" else ""; currentView?.text = currentInput
    }

    private fun calculateBasic() {
        val prev = previousInput.toDoubleOrNull(); val curr = currentInput.toDoubleOrNull()
        if (prev == null || curr == null || operator == null) return
        val res = when (operator) { "+" -> prev + curr; "−" -> prev - curr; "×" -> prev * curr; "÷" -> if (curr == 0.0) Double.NaN else prev / curr; "%" -> (prev / 100.0) * curr; else -> return }
        val formatted = formatResult(res)
        if (formatted != "Error") {
            historyList.add(0, "$previousInput $operator $currentInput = $formatted")
            savePreferences()
        }
        currentInput = formatted; previousInput = ""; operator = null; justCalculated = true
    }

    private fun formatResult(res: Double): String {
        return try { if (res.isNaN() || res.isInfinite()) "Error" else BigDecimal.valueOf(res).setScale(calcPrecision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() } catch (e: Exception) { "Error" }
    }
        private fun createInputBox(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText; setHintTextColor(Color.parseColor("#5C657A")); setTextColor(Color.WHITE)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED; textSize = 17f
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            background = GradientDrawable().apply { setColor(getCardColor()); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), getStrokeColor()) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
        }
    }

    private fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView {
        val btnBgColor = if (isPrimary) primaryColor else getCardColor()
        val textColor = if (isPrimary) Color.parseColor("#08090C") else Color.WHITE
        val shape = GradientDrawable().apply { setColor(btnBgColor); cornerRadius = dp(buttonCornerStyle).toFloat(); if (!isPrimary) setStroke(dp(1.5f), getStrokeColor()) }
        return TextView(this).apply {
            text = textStr; gravity = Gravity.CENTER; setTextColor(textColor); textSize = 17f; typeface = Typeface.DEFAULT_BOLD
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)
            setPadding(0, dp(16f), 0, dp(16f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }
            isClickable = true; setOnClickListener { feedback(it); onClick(it) }
        }
    }

    private fun createResultTextView(): TextView = TextView(this).apply {
        setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(20f), 0, 0)
        setOnClickListener { if (text.isNotEmpty()) { feedback(it); copyToClipboard(text.toString()) } }
    }

    private fun createVatCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val amountInput = createInputBox("סכום (₪)"); val vatInput = createInputBox("אחוז מע״מ (ברירת מחדל 18%)"); val res = createResultTextView()
        val addBtn = createActionBtn("הוסף מע״מ לסכום (+)") {
            val a = amountInput.text.toString().toDoubleOrNull(); val v = vatInput.text.toString().toDoubleOrNull() ?: 18.0
            if (a != null) { val vat = a * (v / 100); res.text = "סכום מע״מ: ₪${formatCurrency(vat)}\nסה״כ כולל מע״מ: ₪${formatCurrency(a + vat)}" }
        }
        val removeBtn = createActionBtn("חלץ מע״מ מסכום כולל (-)", false) {
            val a = amountInput.text.toString().toDoubleOrNull(); val v = vatInput.text.toString().toDoubleOrNull() ?: 18.0
            if (a != null) { val before = a / (1 + (v / 100)); res.text = "לפני מע״מ: ₪${formatCurrency(before)}\nרכיב המע״מ: ₪${formatCurrency(a - before)}" }
        }
        root.addView(amountInput); root.addView(vatInput); root.addView(addBtn); root.addView(removeBtn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createLoanCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val amountIn = createInputBox("סכום ההלוואה (₪)"); val rateIn = createInputBox("ריבית שנתית (%) - למשל 5.5"); val monthsIn = createInputBox("תקופה בחודשים - למשל 60"); val res = createResultTextView()
        val btn = createActionBtn("חשב החזר הלוואה") {
            val p = amountIn.text.toString().toDoubleOrNull(); val rYear = rateIn.text.toString().toDoubleOrNull(); val n = monthsIn.text.toString().toIntOrNull()
            if (p != null && rYear != null && n != null && n > 0) {
                val r = (rYear / 100.0) / 12.0
                val monthly = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
                val total = monthly * n
                res.text = "החזר חודשי: ₪${formatCurrency(monthly)}\nסה״כ תשלומים: ₪${formatCurrency(total)}\nסה״כ ריבית שתשולם: ₪${formatCurrency(total - p)}"
            } else res.text = "נא להזין נתונים תקינים."
        }
        root.addView(amountIn); root.addView(rateIn); root.addView(monthsIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createDistanceCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val kmInput = createInputBox("מרחק בקילומטרים (ק״מ)"); val res = createResultTextView()
        val btn = createActionBtn("המר מרחקים") {
            val km = kmInput.text.toString().toDoubleOrNull()
            if (km != null) res.text = "מיילים: ${formatCurrency(km * 0.621371)}\nמטרים: ${formatCurrency(km * 1000)}\nרגל (Feet): ${formatCurrency(km * 3280.84)}\nאינצ׳ים: ${formatCurrency(km * 39370.1)}"
        }
        root.addView(kmInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createWeightCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val kgInput = createInputBox("משקל בקילוגרמים (ק״ג)"); val res = createResultTextView()
        val btn = createActionBtn("המר משקלים") {
            val kg = kgInput.text.toString().toDoubleOrNull()
            if (kg != null) res.text = "גרם: ${formatCurrency(kg * 1000)}\nפאונד (lbs): ${formatCurrency(kg * 2.20462)}\nאונקיות (oz): ${formatCurrency(kg * 35.274)}\nטון: ${formatCurrency(kg / 1000)}"
        }
        root.addView(kgInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createTempCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val cInput = createInputBox("טמפרטורה בצלזיוס (°C)"); val res = createResultTextView()
        val btn = createActionBtn("המר טמפרטורה") {
            val c = cInput.text.toString().toDoubleOrNull()
            if (c != null) res.text = "פרנהייט (°F): ${formatCurrency(c * 9 / 5 + 32)}\nקלווין (K): ${formatCurrency(c + 273.15)}"
        }
        root.addView(cInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createProgrammerCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val numInput = createInputBox("מספר שלם עשרוני (DEC)"); val res = createResultTextView()
        val btn = createActionBtn("המר בסיסי ספירה") {
            val n = numInput.text.toString().toLongOrNull()
            if (n != null) res.text = "בינארי (BIN):\n${java.lang.Long.toBinaryString(n)}\n\nהקסדצימלי (HEX):\n${java.lang.Long.toHexString(n).uppercase()}\n\nאוקטלי (OCT):\n${java.lang.Long.toOctalString(n)}"
            else res.text = "נא להזין מספר שלם."
        }
        root.addView(numInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createPercentageCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val pctInput = createInputBox("אחוז (למשל 15)"); val totalInput = createInputBox("מתוך סכום (למשל 200)"); val res = createResultTextView()
        val btn = createActionBtn("חשב אחוז") {
            val pct = pctInput.text.toString().toDoubleOrNull(); val total = totalInput.text.toString().toDoubleOrNull()
            if (pct != null && total != null) res.text = "$pct% מתוך $total הם:\n\n${formatCurrency(total * (pct / 100.0))}"
        }
        root.addView(pctInput); root.addView(totalInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createDiscountCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val priceInput = createInputBox("מחיר מקורי (₪)"); val discountInput = createInputBox("אחוז הנחה (%)"); val res = createResultTextView()
        val btn = createActionBtn("חשב מחיר סופי") {
            val p = priceInput.text.toString().toDoubleOrNull(); val d = discountInput.text.toString().toDoubleOrNull()
            if (p != null && d != null) { val s = p * (d / 100); res.text = "מחיר סופי: ₪${formatCurrency(p - s)}\nחסכת: ₪${formatCurrency(s)}" }
        }
        root.addView(priceInput); root.addView(discountInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createTipCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val billIn = createInputBox("סכום החשבון (₪)"); val tipIn = createInputBox("אחוז טיפ (ברירת מחדל 10%)"); val pplIn = createInputBox("מספר סועדים (ברירת מחדל 1)"); val res = createResultTextView()
        val btn = createActionBtn("חשב טיפ") {
            val b = billIn.text.toString().toDoubleOrNull(); val t = tipIn.text.toString().toDoubleOrNull() ?: 10.0; val p = pplIn.text.toString().toIntOrNull() ?: 1
            if (b != null && p > 0) { val tip = b * (t / 100); val tot = b + tip; res.text = "טיפ: ₪${formatCurrency(tip)}\nסה״כ: ₪${formatCurrency(tot)}\nלכל סועד: ₪${formatCurrency(tot / p)}" }
        }
        root.addView(billIn); root.addView(tipIn); root.addView(pplIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createBMICalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val wIn = createInputBox("משקל (ק״ג)"); val hIn = createInputBox("גובה (ס״מ)"); val res = createResultTextView()
        val btn = createActionBtn("חשב מדד BMI") {
            val w = wIn.text.toString().toDoubleOrNull(); val h = hIn.text.toString().toDoubleOrNull()
            if (w != null && h != null && h > 0) { val bmi = w / ((h / 100) * (h / 100)); val st = when { bmi < 18.5 -> "תת משקל 🔹"; bmi < 25 -> "משקל תקין ✅"; bmi < 30 -> "עודף משקל 🔸"; else -> "השמנת יתר ⚠️" }; res.text = String.format("ה-BMI שלך: %.1f\n%s", bmi, st) }
        }
        root.addView(wIn); root.addView(hIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createAgeCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val res = createResultTextView()
        val btn = createActionBtn("בחר תאריך לידה 📅") {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val t = Calendar.getInstance(); var ay = t.get(Calendar.YEAR) - y; var am = t.get(Calendar.MONTH) - m; var ad = t.get(Calendar.DAY_OF_MONTH) - d
                if (ad < 0) { am--; ad += t.getActualMaximum(Calendar.DAY_OF_MONTH) }
                if (am < 0) { ay--; am += 12 }
                res.text = if (ay < 0) "תאריך עתידי!" else "הגיל שלך:\n$ay שנים, $am חודשים ו-$ad ימים"
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createSettingsPage(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(0f), dp(20f), dp(20f)) }
        val titleColor = TextView(this).apply { text = "🎨 צבע נושא כללי (נשמר אוטומטית)"; setTextColor(Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(10f), 0, dp(10f)) }
        val colorScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) } }
        val colorContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (hue in 0..350 step 15) {
            val c = Color.HSVToColor(floatArrayOf(hue.toFloat(), 0.7f, 0.95f))
            val circle = View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c); if (c == primaryColor) setStroke(dp(3f), Color.WHITE) }
                layoutParams = LinearLayout.LayoutParams(dp(50f), dp(50f)).apply { setMargins(dp(5f), dp(5f), dp(5f), dp(5f)) }
                setOnClickListener { feedback(it); primaryColor = c; savePreferences(); applyTheme() }
            }
            colorContainer.addView(circle)
        }
        colorScroll.addView(colorContainer)

        val titleBg = TextView(this).apply { text = "🌌 צבע רקע לאפליקציה"; setTextColor(Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(10f), 0, dp(10f)) }
        val bgScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16f) } }
        val bgContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (hex in listOf("#08090C", "#121212", "#0F172A", "#1C1917", "#311025", "#064E3B", "#172554", "#2E1065")) {
            val c = Color.parseColor(hex)
            val circle = View(this).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(c); setStroke(dp(1.5f), Color.parseColor("#444444")); if (c == bgColor) setStroke(dp(3f), Color.WHITE) }
                layoutParams = LinearLayout.LayoutParams(dp(50f), dp(50f)).apply { setMargins(dp(5f), dp(5f), dp(5f), dp(5f)) }
                setOnClickListener { feedback(it); bgColor = c; savePreferences(); applyTheme() }
            }
            bgContainer.addView(circle)
        }
        bgScroll.addView(bgContainer)

        val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", false) { useHaptic = !useHaptic; savePreferences(); applyTheme() }
        val sndBtn = createActionBtn(if (useSound) "צלילי הקשה: מופעל 🔊" else "צלילי הקשה: כבוי 🔇", false) { useSound = !useSound; savePreferences(); applyTheme() }
        val scrBtn = createActionBtn(if (keepScreenOn) "מסך תמיד דולק: מופעל 🔆" else "מסך תמיד דולק: כבוי 🌙", false) { keepScreenOn = !keepScreenOn; updateKeepScreenOn(); savePreferences(); applyTheme() }
        val shapeName = when(buttonCornerStyle) { 8f -> "מרובעים ⏹️"; 24f -> "מודרניים 🔲"; else -> "עגולים ⚪" }
        val shapeBtn = createActionBtn("צורת כפתורים: $shapeName", false) { buttonCornerStyle = when(buttonCornerStyle) { 8f -> 24f; 24f -> 42f; else -> 8f }; savePreferences(); applyTheme() }
        val precBtn = createActionBtn("דיוק עשרוני: $calcPrecision ספרות ⚙️", false) { calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2; savePreferences(); applyTheme() }
        val backBtn = createActionBtn("חזור למחשבון 🔙", true) { switchTab(0) }

        root.addView(titleColor); root.addView(colorScroll); root.addView(titleBg); root.addView(bgScroll)
        root.addView(vibBtn); root.addView(sndBtn); root.addView(scrBtn); root.addView(shapeBtn); root.addView(precBtn); root.addView(backBtn)
        scroll.addView(root); return scroll
    }

    private fun formatCurrency(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    private fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
