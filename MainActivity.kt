package com.example.calculator

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread
import kotlin.math.pow

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var rootFrame: FrameLayout
    private lateinit var mainContent: LinearLayout
    private lateinit var contentFrame: FrameLayout
    private lateinit var dimView: View
    private lateinit var drawerMenu: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var menuBtn: TextView
    private val drawerItemsViews = mutableListOf<View>()

    private var useHaptic = true; private var useSound = true; private var keepScreenOn = false
    private var calcPrecision = 8; private var buttonCornerStyle = 24f
    private var primaryColor = Color.parseColor("#64B5F6"); private var bgColor = Color.parseColor("#08090C")
    private var memoryValue = 0.0; private var currentTabIndex = 0
    private val historyList = mutableListOf<String>(); private var isDrawerOpen = false

    private val categories = listOf(
        "🧮 מחשבון רגיל", "🔬 מחשבון מדעי fx991", "🧾 מחשבון מע״מ", "🏦 מחשבון הלוואה",
        "📏 המרת מרחקים", "⚖️ המרת משקלים", "🌡️ המרת טמפ׳", "💻 מחשבון מתכנתים",
        "📊 חישוב אחוזים", "🏷️ חישוב הנחות", "💰 חלוקת טיפים", "🏃 מדד BMI", "🎂 חישוב גיל",
        "💵 המרת מטבעות לייב", "🔥 מחשבון BMR", "➗ משוואות ריבועיות", "⅟ מחשבון שברים", "⚙️ הגדרות וערכות נושא"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("CalcProPrefs", Context.MODE_PRIVATE); loadPreferences()

        rootFrame = FrameLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        mainContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
        
        val topHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(16f)) }
        menuBtn = TextView(this).apply { text = "☰"; setTextColor(Color.WHITE); textSize = 28f; setPadding(0, 0, dp(16f), 0); setOnClickListener { feedback(it); openDrawer() } }
        titleText = TextView(this).apply { text = "מחשבון פרימיום"; setTextColor(Color.WHITE); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        val historyBtn = TextView(this).apply { text = "🕒"; textSize = 20f; gravity = Gravity.CENTER; setPadding(dp(8f), dp(8f), dp(8f), dp(8f)); setOnClickListener { feedback(it); showHistoryDialog() } }

        topHeader.addView(menuBtn); topHeader.addView(titleText); topHeader.addView(historyBtn)
        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        mainContent.addView(topHeader); mainContent.addView(contentFrame)

        dimView = View(this).apply { setBackgroundColor(Color.parseColor("#B3000000")); visibility = View.GONE; alpha = 0f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); setOnClickListener { if (isDrawerOpen) closeDrawer() } }
        val drawerScroll = ScrollView(this).apply { layoutParams = FrameLayout.LayoutParams(dp(280f), FrameLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.RIGHT }; isClickable = true }
        drawerMenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(30f), 0, dp(30f)) }

        val appLogo = TextView(this).apply { text = "אולר שווייצרי\nPro fx-991"; setTextColor(Color.WHITE); textSize = 24f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(30f)) }
        drawerMenu.addView(appLogo)

        for ((index, title) in categories.withIndex()) {
            val item = TextView(this).apply { text = title; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(24f), dp(16f), dp(24f), dp(16f)); setOnClickListener { feedback(it); switchTab(index); closeDrawer() } }
            drawerItemsViews.add(item); drawerMenu.addView(item)
        }

        drawerScroll.addView(drawerMenu)
        rootFrame.addView(mainContent); rootFrame.addView(dimView); rootFrame.addView(drawerScroll)
        drawerScroll.translationX = dp(280f).toFloat()

        setContentView(rootFrame)
        setupShortcuts()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_CALC" -> { currentTabIndex = 0; applyTheme() }
            "ACTION_SCI" -> { currentTabIndex = 1; applyTheme() }
            "ACTION_COIN" -> { currentTabIndex = 13; applyTheme() }
            else -> applyTheme()
        }
    }

    private fun setupShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            val sm = getSystemService(ShortcutManager::class.java)
            val s1 = ShortcutInfo.Builder(this, "coin").setShortLabel("מטבעות לייב").setIcon(Icon.createWithResource(this, android.R.drawable.star_on)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_COIN" }).build()
            val s2 = ShortcutInfo.Builder(this, "sci").setShortLabel("מחשבון מדעי").setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_edit)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_SCI" }).build()
            sm.dynamicShortcuts = listOf(s1, s2)
        }
    }
        private fun loadPreferences() {
        useHaptic = prefs.getBoolean("useHaptic", true); useSound = prefs.getBoolean("useSound", true); keepScreenOn = prefs.getBoolean("keepScreenOn", false)
        calcPrecision = prefs.getInt("calcPrecision", 8); buttonCornerStyle = prefs.getFloat("buttonCornerStyle", 24f)
        primaryColor = prefs.getInt("primaryColor", Color.parseColor("#64B5F6")); bgColor = prefs.getInt("bgColor", Color.parseColor("#08090C"))
        memoryValue = Double.fromBits(prefs.getLong("memoryValue", 0L)); val savedHist = prefs.getString("history", "") ?: ""
        historyList.clear(); if (savedHist.isNotEmpty()) historyList.addAll(savedHist.split("||")); updateKeepScreenOn()
    }

    private fun savePreferences() { prefs.edit().putBoolean("useHaptic", useHaptic).putBoolean("useSound", useSound).putBoolean("keepScreenOn", keepScreenOn).putInt("calcPrecision", calcPrecision).putFloat("buttonCornerStyle", buttonCornerStyle).putInt("primaryColor", primaryColor).putInt("bgColor", bgColor).putLong("memoryValue", memoryValue.toRawBits()).putString("history", historyList.take(30).joinToString("||")).apply() }
    private fun updateKeepScreenOn() { if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    private fun applyTheme() {
        window.statusBarColor = bgColor; window.navigationBarColor = bgColor; mainContent.setBackgroundColor(bgColor); drawerMenu.setBackgroundColor(getCardColor())
        updateDrawerSelection(); switchTab(currentTabIndex)
    }

    private fun openDrawer() { val drawer = rootFrame.getChildAt(2); dimView.visibility = View.VISIBLE; dimView.animate().alpha(1f).setDuration(250).start(); drawer.animate().translationX(0f).setDuration(250).setInterpolator(android.view.animation.DecelerateInterpolator()).start(); isDrawerOpen = true }
    private fun closeDrawer() { val drawer = rootFrame.getChildAt(2); dimView.animate().alpha(0f).setDuration(250).withEndAction { dimView.visibility = View.GONE }.start(); drawer.animate().translationX(dp(280f).toFloat()).setDuration(250).setInterpolator(android.view.animation.AccelerateInterpolator()).start(); isDrawerOpen = false }

    private fun getCardColor(): Int = lightenColor(bgColor, 0.06f)
    private fun getStrokeColor(): Int = lightenColor(bgColor, 0.15f)
    private fun lightenColor(color: Int, fraction: Float): Int { val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color); return Color.rgb((r + (255 - r) * fraction).toInt(), (g + (255 - g) * fraction).toInt(), (b + (255 - b) * fraction).toInt()) }

    private fun feedback(view: View) { if (useHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); if (useSound) view.playSoundEffect(SoundEffectConstants.CLICK) }
    private fun copyToClipboard(text: String) { val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("CalcResult", text)); Toast.makeText(this, "הועתק: $text 📋", Toast.LENGTH_SHORT).show() }

    private fun showHistoryDialog() {
        if (historyList.isEmpty()) { Toast.makeText(this, "היסטוריית החישובים ריקה", Toast.LENGTH_SHORT).show(); return }
        val items = historyList.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("🕒 היסטוריה (לחץ להזנה)").setItems(items) { _, which -> val selected = items[which].substringAfter("=").trim(); currentInput = selected; justCalculated = true; if (currentTabIndex != 0 && currentTabIndex != 1) switchTab(0) else currentView?.text = currentInput }.setNeutralButton("נקה הכל") { _, _ -> historyList.clear(); savePreferences(); Toast.makeText(this, "נמחק", Toast.LENGTH_SHORT).show() }.setPositiveButton("סגור", null).show()
    }

    private fun updateDrawerSelection() {
        for (i in drawerItemsViews.indices) {
            val tv = drawerItemsViews[i] as TextView
            if (i == currentTabIndex) { tv.setBackgroundColor(Color.argb(40, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))); tv.setTextColor(primaryColor) } 
            else { tv.setBackgroundColor(Color.TRANSPARENT); tv.setTextColor(Color.WHITE) }
        }
    }

    private fun switchTab(index: Int) {
        currentTabIndex = index; titleText.text = categories[index].substring(3)
        updateDrawerSelection(); contentFrame.removeAllViews()
        val view = when (index) {
            0 -> createBasicCalculator(); 1 -> createScientificCalculator(); 2 -> createVatCalculator()
            3 -> createLoanCalculator(); 4 -> createDistanceCalculator(); 5 -> createWeightCalculator()
            6 -> createTempCalculator(); 7 -> createProgrammerCalculator(); 8 -> createPercentageCalculator()
            9 -> createDiscountCalculator(); 10 -> createTipCalculator(); 11 -> createBMICalculator()
            12 -> createAgeCalculator(); 13 -> createCurrencyCalculator(); 14 -> createBmrCalculator()
            15 -> createQuadraticCalculator(); 16 -> createFractionCalculator(); 17 -> createSettingsPage()
            else -> createBasicCalculator()
        }
        contentFrame.addView(view)
    }
        private fun evalMathExpression(str: String): Double {
        val expr = str.replace("×", "*").replace("÷", "/").replace("−", "-").replace("π", Math.PI.toString()).replace("e", Math.E.toString()).replace("%", "/100")
        return object : Any() {
            var pos = -1; var ch = 0
            fun nextChar() { ch = if (++pos < expr.length) expr[pos].toInt() else -1 }
            fun eat(charToEat: Int): Boolean { while (ch == ' '.toInt()) nextChar(); if (ch == charToEat) { nextChar(); return true }; return false }
            fun parse(): Double { nextChar(); val x = parseExpression(); if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar()); return x }
            fun parseExpression(): Double { var x = parseTerm(); while (true) { if (eat('+'.toInt())) x += parseTerm() else if (eat('-'.toInt())) x -= parseTerm() else return x } }
            fun parseTerm(): Double { var x = parseFactor(); while (true) { if (eat('*'.toInt())) x *= parseFactor() else if (eat('/'.toInt())) x /= parseFactor() else return x } }
            fun parseFactor(): Double {
                if (eat('+'.toInt())) return parseFactor(); if (eat('-'.toInt())) return -parseFactor()
                var x: Double; val startPos = pos
                if (eat('('.toInt())) { x = parseExpression(); eat(')'.toInt()) }
                else if (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt()) { while (ch >= '0'.toInt() && ch <= '9'.toInt() || ch == '.'.toInt() || ch == 'E'.toInt()) nextChar(); x = expr.substring(startPos, pos).toDouble() } 
                else if (eat('√'.toInt())) { x = Math.sqrt(parseFactor()) }
                else if (ch >= 'a'.toInt() && ch <= 'z'.toInt()) {
                    while (ch >= 'a'.toInt() && ch <= 'z'.toInt()) nextChar(); val func = expr.substring(startPos, pos); x = parseFactor()
                    x = when(func) { "sin" -> Math.sin(Math.toRadians(x)); "cos" -> Math.cos(Math.toRadians(x)); "tan" -> Math.tan(Math.toRadians(x)); "log" -> Math.log10(x); "ln" -> Math.log(x); "sqrt" -> Math.sqrt(x); else -> throw RuntimeException("Unknown: $func") }
                } else throw RuntimeException("Unexpected: " + ch.toChar())
                if (eat('^'.toInt())) x = Math.pow(x, parseFactor())
                if (eat('!'.toInt())) { var fact = 1.0; for (i in 2..x.toInt()) fact *= i; x = fact }
                return x
            }
        }.parse()
    }

    private var currentInput = ""
    private var justCalculated = false
    private var expressionView: TextView? = null
    private var currentView: TextView? = null
    private var memoryIndicator: TextView? = null

    private fun createBasicCalculator(): View = createCalculatorLayout(listOf(listOf("MC", "MR", "M+", "M-"), listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("(", "0", ".", "=")))
    private fun createScientificCalculator(): View = createCalculatorLayout(listOf(listOf("sin", "cos", "tan", "log", "ln"), listOf("√", "x²", "x³", "x^y", "!"), listOf("π", "e", "(", ")", "÷"), listOf("7", "8", "9", "%", "×"), listOf("4", "5", "6", "MC", "−"), listOf("1", "2", "3", "MR", "+"), listOf("AC", "0", ".", "⌫", "=")), isGrid5x7 = true)

    private fun createCalculatorLayout(rows: List<List<String>>, isGrid5x7: Boolean = false): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(14f), dp(0f), dp(14f), dp(14f)) }
        val displayCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(18f), dp(14f), dp(18f), dp(14f)); background = GradientDrawable().apply { setColor(getCardColor()); cornerRadius = dp(28f).toFloat(); setStroke(dp(1.5f), getStrokeColor()) }; setOnClickListener { feedback(it); copyToClipboard(currentInput) } }
        memoryIndicator = TextView(this).apply { text = if (memoryValue != 0.0) "M = ${formatResult(memoryValue)}" else ""; setTextColor(primaryColor); textSize = 11f; gravity = Gravity.START }
        expressionView = TextView(this).apply { text = ""; setTextColor(Color.parseColor("#8A94A8")); setAutoSizeTextTypeUniformWithConfiguration(12, 22, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        currentView = TextView(this).apply { text = if(currentInput.isEmpty()) "0" else currentInput; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setAutoSizeTextTypeUniformWithConfiguration(22, 56, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f) }
        displayCard.addView(memoryIndicator); displayCard.addView(expressionView); displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.28f else 0.32f).apply { bottomMargin = dp(12f) })

        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (symbol in rowSymbols) { val weight = if (!isGrid5x7 && symbol == "0") 2f else 1f; rowLayout.addView(createAnimatedButton(symbol, isGrid5x7), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply { setMargins(dp(3f), dp(3f), dp(3f), dp(3f)) }) }
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.72f else 0.68f)); return root
    }

    private fun createAnimatedButton(symbol: String, isSmall: Boolean): TextView {
        val isMem = symbol in listOf("MC", "MR", "M+", "M-"); val isOp = symbol in listOf("÷", "×", "−", "+", "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e", "(", ")")
        val defaultBg = when { symbol == "AC" -> lightenColor(Color.RED, 0.2f); symbol == "=" -> primaryColor; isMem -> lightenColor(bgColor, 0.03f); symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> lightenColor(bgColor, 0.1f); else -> getCardColor() }
        val strokeColor = if (symbol == "=" || symbol == "AC") Color.TRANSPARENT else getStrokeColor()
        val textColor = when { symbol == "AC" -> Color.WHITE; symbol == "=" -> Color.parseColor("#08090C"); isMem || symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> primaryColor; else -> Color.WHITE }
        val shape = GradientDrawable().apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(buttonCornerStyle).toFloat(); setColor(defaultBg); setStroke(dp(1.5f), strokeColor) }

        return TextView(this).apply {
            text = symbol; setTextColor(textColor); setAutoSizeTextTypeUniformWithConfiguration(10, if(isSmall) 22 else 28, 2, TypedValue.COMPLEX_UNIT_SP); typeface = if (symbol == "=" || isOp || isMem) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL); gravity = Gravity.CENTER; background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); isClickable = true
            setOnTouchListener { v, e -> when (e.actionMasked) { MotionEvent.ACTION_DOWN -> { feedback(v); v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).start() }; MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); if (e.actionMasked == MotionEvent.ACTION_UP) onEquationButtonClick(symbol) } }; false }
        }
    }

    private fun onEquationButtonClick(s: String) {
        if (justCalculated && s !in listOf("÷", "×", "−", "+", "^", "%")) { currentInput = ""; justCalculated = false }
        justCalculated = false
        when (s) {
            "MC" -> { memoryValue = 0.0; savePreferences() }; "MR" -> { currentInput += formatResult(memoryValue) }
            "M+" -> { try { memoryValue += evalMathExpression(if(currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch(e:Exception){} }
            "M-" -> { try { memoryValue -= evalMathExpression(if(currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch(e:Exception){} }
            "AC" -> { currentInput = ""; expressionView?.text = "" }; "⌫" -> { if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1) }
            "sin", "cos", "tan", "log", "ln" -> { currentInput += "$s(" }; "√" -> { currentInput += "√(" }; "x²" -> { currentInput += "^2" }; "x³" -> { currentInput += "^3" }; "x^y" -> { currentInput += "^" }
            "=" -> { if (currentInput.isNotEmpty()) { try { val res = evalMathExpression(currentInput); val formatted = formatResult(res); expressionView?.text = "$currentInput ="; historyList.add(0, "$currentInput = $formatted"); savePreferences(); currentInput = formatted; justCalculated = true } catch (e: Exception) { currentInput = "Error"; justCalculated = true } } }
            else -> currentInput += s
        }
        memoryIndicator?.text = if (memoryValue != 0.0) "M = ${formatResult(memoryValue)}" else ""; currentView?.text = if(currentInput.isEmpty()) "0" else currentInput
    }

    private fun formatResult(res: Double): String { return try { if (res.isNaN() || res.isInfinite()) "Error" else BigDecimal.valueOf(res).setScale(calcPrecision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() } catch (e: Exception) { "Error" } }
        private fun createInputBox(hintText: String): EditText {
        return EditText(this).apply { hint = hintText; setHintTextColor(Color.parseColor("#5C657A")); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED; textSize = 17f; setPadding(dp(18f), dp(18f), dp(18f), dp(18f)); background = GradientDrawable().apply { setColor(getCardColor()); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), getStrokeColor()) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) } }
    }

    private fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView {
        val shape = GradientDrawable().apply { setColor(if (isPrimary) primaryColor else getCardColor()); cornerRadius = dp(buttonCornerStyle).toFloat(); if (!isPrimary) setStroke(dp(1.5f), getStrokeColor()) }
        return TextView(this).apply { text = textStr; gravity = Gravity.CENTER; setTextColor(if (isPrimary) Color.parseColor("#08090C") else Color.WHITE); textSize = 17f; typeface = Typeface.DEFAULT_BOLD; background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); setPadding(0, dp(16f), 0, dp(16f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }; isClickable = true; setOnClickListener { feedback(it); onClick(it) } }
    }

    private fun createResultTextView(): TextView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(20f), 0, 0); setOnClickListener { if (text.isNotEmpty()) { feedback(it); copyToClipboard(text.toString()) } } }

    private fun createCurrencyCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val amountInput = createInputBox("סכום להמרה (₪)"); val res = createResultTextView().apply { textSize = 18f }
        val btn = createActionBtn("💱 משוך שערי מטבע בזמן אמת") {
            val amount = amountInput.text.toString().toDoubleOrNull() ?: 1.0; res.text = "טוען נתונים מהאינטרנט..."
            thread {
                try {
                    val response = URL("https://open.er-api.com/v6/latest/ILS").readText()
                    val json = JSONObject(response); val rates = json.getJSONObject("rates")
                    val usd = 1.0 / rates.getDouble("USD"); val eur = 1.0 / rates.getDouble("EUR"); val gbp = 1.0 / rates.getDouble("GBP")
                    val time = json.getString("time_last_update_utc").substring(0, 16)
                    Handler(Looper.getMainLooper()).post { res.text = "עבור ₪${formatCurrency(amount)}:\n\n🇺🇸 דולר (USD): $${formatCurrency(amount * rates.getDouble("USD"))}\n🇪🇺 אירו (EUR): €${formatCurrency(amount * rates.getDouble("EUR"))}\n🇬🇧 פאונד (GBP): £${formatCurrency(amount * rates.getDouble("GBP"))}\n\nשערי חליפין (₪1 שווה ל...):\nדולר: ₪${formatCurrency(usd)}\nאירו: ₪${formatCurrency(eur)}\nפאונד: ₪${formatCurrency(gbp)}\n\nעודכן ב: $time" }
                } catch (e: Exception) { Handler(Looper.getMainLooper()).post { res.text = "שגיאה בחיבור לאינטרנט.\nודא שיש לך קליטה ונסה שוב." } }
            }
        }
        root.addView(amountInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createVatCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val amountInput = createInputBox("סכום (₪)"); val vatInput = createInputBox("אחוז מע״מ (ברירת מחדל 18%)"); val res = createResultTextView()
        val addBtn = createActionBtn("הוסף מע״מ לסכום (+)") { val a = amountInput.text.toString().toDoubleOrNull(); val v = vatInput.text.toString().toDoubleOrNull() ?: 18.0; if (a != null) { val vat = a * (v / 100); res.text = "סכום מע״מ: ₪${formatCurrency(vat)}\nסה״כ כולל מע״מ: ₪${formatCurrency(a + vat)}" } }
        val removeBtn = createActionBtn("חלץ מע״מ מסכום כולל (-)", false) { val a = amountInput.text.toString().toDoubleOrNull(); val v = vatInput.text.toString().toDoubleOrNull() ?: 18.0; if (a != null) { val before = a / (1 + (v / 100)); res.text = "לפני מע״מ: ₪${formatCurrency(before)}\nרכיב המע״מ: ₪${formatCurrency(a - before)}" } }
        root.addView(amountInput); root.addView(vatInput); root.addView(addBtn); root.addView(removeBtn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createLoanCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val amountIn = createInputBox("סכום ההלוואה (₪)"); val rateIn = createInputBox("ריבית שנתית (%)"); val monthsIn = createInputBox("תקופה בחודשים"); val res = createResultTextView()
        val btn = createActionBtn("חשב החזר הלוואה") { val p = amountIn.text.toString().toDoubleOrNull(); val rYear = rateIn.text.toString().toDoubleOrNull(); val n = monthsIn.text.toString().toIntOrNull(); if (p != null && rYear != null && n != null && n > 0) { val r = (rYear / 100.0) / 12.0; val monthly = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1); val total = monthly * n; res.text = "החזר חודשי: ₪${formatCurrency(monthly)}\nסה״כ תשלומים: ₪${formatCurrency(total)}\nסה״כ ריבית: ₪${formatCurrency(total - p)}" } else res.text = "נא להזין נתונים תקינים." }
        root.addView(amountIn); root.addView(rateIn); root.addView(monthsIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createDistanceCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val kmInput = createInputBox("מרחק בקילומטרים (ק״מ)"); val res = createResultTextView()
        val btn = createActionBtn("המר מרחקים") { val km = kmInput.text.toString().toDoubleOrNull(); if (km != null) res.text = "מיילים: ${formatCurrency(km * 0.621371)}\nמטרים: ${formatCurrency(km * 1000)}\nרגל (Feet): ${formatCurrency(km * 3280.84)}" }
        root.addView(kmInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createWeightCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val kgInput = createInputBox("משקל בקילוגרמים (ק״ג)"); val res = createResultTextView()
        val btn = createActionBtn("המר משקלים") { val kg = kgInput.text.toString().toDoubleOrNull(); if (kg != null) res.text = "גרם: ${formatCurrency(kg * 1000)}\nפאונד (lbs): ${formatCurrency(kg * 2.20462)}\nאונקיות (oz): ${formatCurrency(kg * 35.274)}" }
        root.addView(kgInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createTempCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val cInput = createInputBox("טמפרטורה בצלזיוס (°C)"); val res = createResultTextView()
        val btn = createActionBtn("המר טמפרטורה") { val c = cInput.text.toString().toDoubleOrNull(); if (c != null) res.text = "פרנהייט (°F): ${formatCurrency(c * 9 / 5 + 32)}\nקלווין (K): ${formatCurrency(c + 273.15)}" }
        root.addView(cInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createProgrammerCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val numInput = createInputBox("מספר שלם עשרוני (DEC)"); val res = createResultTextView()
        val btn = createActionBtn("המר בסיסי ספירה") { val n = numInput.text.toString().toLongOrNull(); if (n != null) res.text = "בינארי (BIN):\n${java.lang.Long.toBinaryString(n)}\n\nהקסדצימלי (HEX):\n${java.lang.Long.toHexString(n).uppercase()}" else res.text = "נא להזין מספר שלם." }
        root.addView(numInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }
        private fun createPercentageCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val pctInput = createInputBox("אחוז (למשל 15)"); val totalInput = createInputBox("מתוך סכום (למשל 200)"); val res = createResultTextView()
        val btn = createActionBtn("חשב אחוז") { val pct = pctInput.text.toString().toDoubleOrNull(); val total = totalInput.text.toString().toDoubleOrNull(); if (pct != null && total != null) res.text = "$pct% מתוך $total הם:\n\n${formatCurrency(total * (pct / 100.0))}" }
        root.addView(pctInput); root.addView(totalInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createDiscountCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val priceInput = createInputBox("מחיר מקורי (₪)"); val discountInput = createInputBox("אחוז הנחה (%)"); val res = createResultTextView()
        val btn = createActionBtn("חשב מחיר סופי") { val p = priceInput.text.toString().toDoubleOrNull(); val d = discountInput.text.toString().toDoubleOrNull(); if (p != null && d != null) { val s = p * (d / 100); res.text = "מחיר סופי: ₪${formatCurrency(p - s)}\nחסכת: ₪${formatCurrency(s)}" } }
        root.addView(priceInput); root.addView(discountInput); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createTipCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val billIn = createInputBox("סכום החשבון (₪)"); val tipIn = createInputBox("אחוז טיפ (ברירת מחדל 10%)"); val pplIn = createInputBox("מספר סועדים (ברירת מחדל 1)"); val res = createResultTextView()
        val btn = createActionBtn("חשב טיפ") { val b = billIn.text.toString().toDoubleOrNull(); val t = tipIn.text.toString().toDoubleOrNull() ?: 10.0; val p = pplIn.text.toString().toIntOrNull() ?: 1; if (b != null && p > 0) { val tip = b * (t / 100); val tot = b + tip; res.text = "טיפ: ₪${formatCurrency(tip)}\nסה״כ: ₪${formatCurrency(tot)}\nלכל סועד: ₪${formatCurrency(tot / p)}" } }
        root.addView(billIn); root.addView(tipIn); root.addView(pplIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createBMICalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val wIn = createInputBox("משקל (ק״ג)"); val hIn = createInputBox("גובה (ס״מ)"); val res = createResultTextView()
        val btn = createActionBtn("חשב מדד BMI") { val w = wIn.text.toString().toDoubleOrNull(); val h = hIn.text.toString().toDoubleOrNull(); if (w != null && h != null && h > 0) { val bmi = w / ((h / 100) * (h / 100)); val st = when { bmi < 18.5 -> "תת משקל 🔹"; bmi < 25 -> "משקל תקין ✅"; bmi < 30 -> "עודף משקל 🔸"; else -> "השמנת יתר ⚠️" }; res.text = String.format("ה-BMI שלך: %.1f\n%s", bmi, st) } }
        root.addView(wIn); root.addView(hIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createAgeCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val res = createResultTextView()
        val btn = createActionBtn("בחר תאריך לידה 📅") { val c = Calendar.getInstance(); DatePickerDialog(this, { _, y, m, d -> val t = Calendar.getInstance(); var ay = t.get(Calendar.YEAR) - y; var am = t.get(Calendar.MONTH) - m; var ad = t.get(Calendar.DAY_OF_MONTH) - d; if (ad < 0) { am--; ad += t.getActualMaximum(Calendar.DAY_OF_MONTH) }; if (am < 0) { ay--; am += 12 }; res.text = if (ay < 0) "תאריך עתידי!" else "הגיל שלך:\n$ay שנים, $am חודשים ו-$ad ימים" }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show() }
        root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createBmrCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val wIn = createInputBox("משקל (ק״ג)"); val hIn = createInputBox("גובה (ס״מ)"); val aIn = createInputBox("גיל (שנים)"); val res = createResultTextView()
        val btnMale = createActionBtn("חשב עבור גבר 👨", true) { val w=wIn.text.toString().toDoubleOrNull(); val h=hIn.text.toString().toDoubleOrNull(); val a=aIn.text.toString().toDoubleOrNull(); if(w!=null&&h!=null&&a!=null) { val bmr=10*w + 6.25*h - 5*a + 5; res.text="BMR (בסיסי): ${formatCurrency(bmr)} קלוריות\nפעילות קלה: ${formatCurrency(bmr*1.2)}\nפעילות רבה: ${formatCurrency(bmr*1.55)}" } else res.text="נא להזין נתונים תקינים." }
        val btnFemale = createActionBtn("חשב עבור אישה 👩", false) { val w=wIn.text.toString().toDoubleOrNull(); val h=hIn.text.toString().toDoubleOrNull(); val a=aIn.text.toString().toDoubleOrNull(); if(w!=null&&h!=null&&a!=null) { val bmr=10*w + 6.25*h - 5*a - 161; res.text="BMR (בסיסי): ${formatCurrency(bmr)} קלוריות\nפעילות קלה: ${formatCurrency(bmr*1.2)}\nפעילות רבה: ${formatCurrency(bmr*1.55)}" } else res.text="נא להזין נתונים תקינים." }
        root.addView(wIn); root.addView(hIn); root.addView(aIn); root.addView(btnMale); root.addView(btnFemale); root.addView(res); scroll.addView(root); return scroll
    }

    private fun createQuadraticCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val aIn = createInputBox("מקדם A (לדוגמה 1)"); val bIn = createInputBox("מקדם B (לדוגמה -5)"); val cIn = createInputBox("מקדם C (לדוגמה 6)"); val res = createResultTextView()
        val btn = createActionBtn("פתור משוואה ריבועית") { val a=aIn.text.toString().toDoubleOrNull(); val b=bIn.text.toString().toDoubleOrNull(); val c=cIn.text.toString().toDoubleOrNull(); if(a!=null&&b!=null&&c!=null) { if(a==0.0) { res.text="משוואה ממעלה ראשונה (A לא יכול להיות 0)." } else { val delta=b*b - 4*a*c; if(delta>0) { val x1=(-b+Math.sqrt(delta))/(2*a); val x2=(-b-Math.sqrt(delta))/(2*a); res.text="שני פתרונות:\nX1 = ${formatCurrency(x1)}\nX2 = ${formatCurrency(x2)}" } else if(delta==0.0) { val x=-b/(2*a); res.text="פתרון יחיד:\nX = ${formatCurrency(x)}" } else { res.text="אין פתרונות ממשיים (Δ < 0)" } } } else res.text="נא להזין נתונים תקינים." }
        root.addView(aIn); root.addView(bIn); root.addView(cIn); root.addView(btn); root.addView(res); scroll.addView(root); return scroll
    }

    private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    private fun createFractionCalculator(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
        val n1 = createInputBox("מונה שבר ראשון"); val d1 = createInputBox("מכנה שבר ראשון"); val n2 = createInputBox("מונה שבר שני"); val d2 = createInputBox("מכנה שבר שני"); val res = createResultTextView()
        fun calc(op: String) { val num1=n1.text.toString().toLongOrNull(); val den1=d1.text.toString().toLongOrNull(); val num2=n2.text.toString().toLongOrNull(); val den2=d2.text.toString().toLongOrNull(); if(num1!=null&&den1!=null&&den1!=0L&&num2!=null&&den2!=null&&den2!=0L) { var rNum=0L; var rDen=1L; when(op){ "+"->{rNum=num1*den2+num2*den1; rDen=den1*den2}; "-"->{rNum=num1*den2-num2*den1; rDen=den1*den2}; "*"->{rNum=num1*num2; rDen=den1*den2}; "/"->{rNum=num1*den2; rDen=den1*num2} }; if(rDen==0L) { res.text="חלוקה באפס!"; return }; val g=Math.abs(gcd(rNum, rDen)); rNum/=g; rDen/=g; if(rDen<0) { rNum=-rNum; rDen=-rDen }; res.text=if(rDen==1L) "תוצאה מצומצמת: $rNum" else "תוצאה מצומצמת: $rNum / $rDen" } else res.text="נתונים לא תקינים (המכנה לא יכול להיות 0)" }
        val btnAdd = createActionBtn("חיבור שברים (+)", true) { calc("+") }; val btnSub = createActionBtn("חיסור שברים (-)", false) { calc("-") }; val btnMul = createActionBtn("כפל שברים (×)", true) { calc("*") }; val btnDiv = createActionBtn("חילוק שברים (÷)", false) { calc("/") }
        root.addView(n1); root.addView(d1); root.addView(n2); root.addView(d2); root.addView(btnAdd); root.addView(btnSub); root.addView(btnMul); root.addView(btnDiv); root.addView(res); scroll.addView(root); return scroll
    }
        // =========================================================================
    // הגדרות, ערכות נושא מובנות וגלגל הצבעים הפתוח 360°
    // =========================================================================
    private fun createSettingsPage(): View {
        val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(0f), dp(20f), dp(24f)) }
        val titleThemes = TextView(this).apply { text = "🎨 ערכות נושא מוכנות (Themes)"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)) }
        val themeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16f) } }
        val themeContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val presetThemes = listOf(Triple("OLED", "#64B5F6", "#08090C"), Triple("Matrix", "#00E676", "#0A190F"), Triple("Barbie", "#FF4081", "#2A0A18"), Triple("Ocean", "#00B4D8", "#0A1128"), Triple("Sunset", "#FFB703", "#1A0F0A"), Triple("Dracula", "#BD93F9", "#282A36"), Triple("Cyber", "#FF007F", "#120458"))
        for (theme in presetThemes) {
            val tName = theme.first; val tPri = Color.parseColor(theme.second); val tBg = Color.parseColor(theme.third)
            val btn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12f), dp(12f), dp(12f), dp(12f)); background = GradientDrawable().apply { setColor(tBg); cornerRadius = dp(16f).toFloat(); setStroke(dp(2f), tPri) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6f), dp(6f), dp(6f), dp(6f)) }; addView(TextView(this@MainActivity).apply { text = tName; setTextColor(tPri); textSize = 14f; typeface = Typeface.DEFAULT_BOLD }); setOnClickListener { feedback(it); primaryColor = tPri; bgColor = tBg; savePreferences(); applyTheme() } }
            themeContainer.addView(btn)
        }
        themeScroll.addView(themeContainer)

        var editingTarget = 0 
        val targetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f); bottomMargin = dp(14f) } }
        lateinit var btnEditPrimary: TextView; lateinit var btnEditBg: TextView; lateinit var pickerContainer: FrameLayout

        fun refreshTargetButtons() {
            btnEditPrimary.background = GradientDrawable().apply { setColor(if (editingTarget == 0) primaryColor else getCardColor()); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 0) Color.WHITE else getStrokeColor()) }; btnEditPrimary.setTextColor(if (editingTarget == 0) Color.parseColor("#08090C") else Color.WHITE)
            btnEditBg.background = GradientDrawable().apply { setColor(if (editingTarget == 1) primaryColor else getCardColor()); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 1) Color.WHITE else getStrokeColor()) }; btnEditBg.setTextColor(if (editingTarget == 1) Color.parseColor("#08090C") else Color.WHITE)
        }

        fun loadColorPicker() { refreshTargetButtons(); pickerContainer.removeAllViews(); pickerContainer.addView(createFullColorWheelPicker(if (editingTarget == 0) primaryColor else bgColor) { newColor -> if (editingTarget == 0) primaryColor = newColor else bgColor = newColor; savePreferences(); window.statusBarColor = bgColor; window.navigationBarColor = bgColor; mainContent.setBackgroundColor(bgColor); drawerMenu.setBackgroundColor(getCardColor()); updateDrawerSelection(); refreshTargetButtons() }) }
        btnEditPrimary = TextView(this).apply { text = "🎨 צבע נושא מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 0; loadColorPicker() } }
        btnEditBg = TextView(this).apply { text = "🌌 צבע רקע מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 1; loadColorPicker() } }
        targetRow.addView(btnEditPrimary); targetRow.addView(btnEditBg)
        pickerContainer = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(18f) } }
        loadColorPicker()

        val titleSettings = TextView(this).apply { text = "הגדרות מערכת נוספות"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8f), 0, dp(10f)) }
        val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", false) { useHaptic = !useHaptic; savePreferences(); applyTheme() }
        val sndBtn = createActionBtn(if (useSound) "צלילי הקשה: מופעל 🔊" else "צלילי הקשה: כבוי 🔇", false) { useSound = !useSound; savePreferences(); applyTheme() }
        val scrBtn = createActionBtn(if (keepScreenOn) "מסך תמיד דולק: מופעל 🔆" else "מסך תמיד דולק: כבוי 🌙", false) { keepScreenOn = !keepScreenOn; updateKeepScreenOn(); savePreferences(); applyTheme() }
        val shapeName = when(buttonCornerStyle) { 8f -> "מרובעים ⏹️"; 24f -> "מודרניים 🔲"; else -> "עגולים ⚪" }
        val shapeBtn = createActionBtn("צורת כפתורים: $shapeName", false) { buttonCornerStyle = when(buttonCornerStyle) { 8f -> 24f; 24f -> 42f; else -> 8f }; savePreferences(); applyTheme() }
        val precBtn = createActionBtn("דיוק עשרוני: $calcPrecision ספרות ⚙️", false) { calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2; savePreferences(); applyTheme() }

        root.addView(titleThemes); root.addView(themeScroll); root.addView(targetRow); root.addView(pickerContainer); root.addView(titleSettings)
        root.addView(vibBtn); root.addView(sndBtn); root.addView(scrBtn); root.addView(shapeBtn); root.addView(precBtn); scroll.addView(root); return scroll
    }

    private fun createFullColorWheelPicker(initialColor: Int, onColorChanged: (Int) -> Unit): View {
        val hsv = FloatArray(3); Color.colorToHSV(initialColor, hsv); var currentAlpha = Color.alpha(initialColor)
        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(dp(16f), dp(16f), dp(16f), dp(16f)); background = GradientDrawable().apply { setColor(getCardColor()); cornerRadius = dp(24f).toFloat(); setStroke(dp(1.5f), getStrokeColor()) } }
        val previewRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) } }
        val previewCircle = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(44f), dp(44f)).apply { leftMargin = dp(12f) }; background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(initialColor); setStroke(dp(2f), Color.WHITE) } }
        val hexLabel = TextView(this).apply { text = String.format("#%08X", initialColor); setTextColor(Color.WHITE); textSize = 15f; typeface = Typeface.MONOSPACE }
        previewRow.addView(previewCircle); previewRow.addView(hexLabel)

        lateinit var wheelView: ColorWheelView
        fun notifyColorUpdate() { val finalColor = Color.HSVToColor(currentAlpha, hsv); (previewCircle.background as GradientDrawable).setColor(finalColor); hexLabel.text = String.format("#%08X", finalColor); wheelView.updateHSV(hsv[0], hsv[1], hsv[2]); onColorChanged(finalColor) }
        wheelView = ColorWheelView(this, hsv[0], hsv[1], hsv[2]) { h, s -> hsv[0] = h; hsv[1] = s; notifyColorUpdate() }.apply { layoutParams = LinearLayout.LayoutParams(dp(220f), dp(220f)).apply { bottomMargin = dp(14f) } }

        val brightLabel = TextView(this).apply { text = "☀️ בהירות"; setTextColor(Color.WHITE); textSize = 14f }
        val brightSeek = SeekBar(this).apply { max = 100; progress = (hsv[2] * 100).toInt(); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) }; setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { hsv[2] = p.coerceAtLeast(5) / 100f; if (u) notifyColorUpdate() }; override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {} }) }
        val alphaLabel = TextView(this).apply { text = "💧 שקיפות"; setTextColor(Color.WHITE); textSize = 14f }
        val alphaSeek = SeekBar(this).apply { max = 255; progress = currentAlpha; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { currentAlpha = p.coerceAtLeast(35); if (u) notifyColorUpdate() }; override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {} }) }

        card.addView(previewRow); card.addView(wheelView); card.addView(brightLabel); card.addView(brightSeek); card.addView(alphaLabel); card.addView(alphaSeek); return card
    }

    inner class ColorWheelView(ctx: Context, private var hue: Float, private var sat: Float, private var value: Float, private val onHueSatChanged: (Float, Float) -> Unit) : View(ctx) {
        private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG); private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3f).toFloat(); color = Color.WHITE }
        private val selFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private var cx = 0f; private var cy = 0f; private var r = 0f
        fun updateHSV(h: Float, s: Float, v: Float) { hue = h; sat = s; value = v; invalidate() }
        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { super.onSizeChanged(w, h, ow, oh); cx = w / 2f; cy = h / 2f; r = (Math.min(w, h) / 2f) - dp(8f); wheelPaint.shader = ComposeShader(SweepGradient(cx, cy, intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED), null), RadialGradient(cx, cy, r, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP), PorterDuff.Mode.SRC_OVER) }
        override fun onDraw(c: Canvas) {
            super.onDraw(c); c.drawCircle(cx, cy, r, wheelPaint); dimPaint.color = Color.argb(((1f - value) * 255).toInt().coerceIn(0, 255), 0, 0, 0); c.drawCircle(cx, cy, r, dimPaint)
            val rad = Math.toRadians(hue.toDouble()); val d = sat * r; val sx = (cx + d * Math.cos(rad)).toFloat(); val sy = (cy + d * Math.sin(rad)).toFloat()
            selFillPaint.color = Color.HSVToColor(floatArrayOf(hue, sat, value)); c.drawCircle(sx, sy, dp(11f).toFloat(), selFillPaint); c.drawCircle(sx, sy, dp(11f).toFloat(), selPaint)
        }
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_MOVE) {
                parent?.requestDisallowInterceptTouchEvent(true); val dx = e.x - cx; val dy = e.y - cy; sat = (Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / r).coerceIn(0f, 1f)
                var a = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat(); if (a < 0f) a += 360f; hue = a; onHueSatChanged(hue, sat); invalidate(); return true
            }
            return super.onTouchEvent(e)
        }
    }

    private fun formatCurrency(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    private fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
