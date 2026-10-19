package com.example.calculator

import android.app.Activity
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.RippleDrawable
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Rational
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    var prefs: SharedPreferences? = null
    private lateinit var rootFrame: FrameLayout
    var mainContent: LinearLayout? = null
    private lateinit var contentFrame: FrameLayout
    private lateinit var dimView: View
    var drawerMenu: LinearLayout? = null
    private lateinit var topHeader: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var menuBtn: TextView
    private val drawerItemsViews = mutableListOf<View>()
    private var tts: TextToSpeech? = null
    
    private lateinit var soundPool: SoundPool
    var soundIds = IntArray(13)

    var useHaptic = true; var useSound = true; var keepScreenOn = false; var soundType = 0
    var calcPrecision = 8; var buttonCornerStyle = 24f
    var primaryColor = Color.parseColor("#64B5F6"); var bgColor = Color.parseColor("#08090C")
    var memoryValue = 0.0; var currentTabIndex = 0
    var historyList = mutableListOf<String>(); private var isDrawerOpen = false

    var fromCurrency = "USD"; var toCurrency = "ILS"
    var fromDist = "קילומטר (km)"; var toDist = "מייל (mi)"
    var fromWeight = "קילוגרם (kg)"; var toWeight = "פאונד (lbs)"
    var fromTemp = "צלזיוס (°C)"; var toTemp = "פרנהייט (°F)"

    var allCurrencies = mutableListOf(
        CurrencyData.getFullName("USD"), CurrencyData.getFullName("ILS"), CurrencyData.getFullName("EUR")
    )

    private val categories = listOf(
        "🧮 מחשבון רגיל", "🔬 מחשבון מדעי", "🧾 מחשבון מע״מ", "🏦 מחשבון הלוואה",
        "📏 המרת מרחקים", "⚖️ המרת משקלים", "🌡️ המרת טמפ׳", "💻 מחשבון מתכנתים",
        "📊 חישוב אחוזים", "🏷️ חישוב הנחות", "💰 חלוקת טיפים", "🏃 מדד BMI", "🎂 חישוב גיל",
        "💵 מטבעות לייב", "🔥 מחשבון BMR", "➗ משוואות ריבועיות", "⅟ מחשבון שברים", "⚙️ הגדרות"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("CalcProPrefs", Context.MODE_PRIVATE); loadPreferences()
        tts = TextToSpeech(this, this)
        
        // תיקון קריטי: מעבר לערוץ המדיה כדי להבטיח השמעה
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(audioAttributes).build()
        for (i in 1..12) {
            val rId = resources.getIdentifier("sound$i", "raw", packageName)
            if (rId != 0) soundIds[i] = soundPool.load(this, rId, 1)
        }

        thread {
            try {
                val res = URL("https://open.er-api.com/v6/latest/USD").readText()
                val json = JSONObject(res); val rates = json.getJSONObject("rates"); val fetched = mutableListOf<String>()
                val keys = rates.keys(); while (keys.hasNext()) fetched.add(CurrencyData.getFullName(keys.next()))
                fetched.sort()
                Handler(Looper.getMainLooper()).post { allCurrencies.clear(); allCurrencies.addAll(fetched) }
            } catch (e: Exception) {}
        }

        rootFrame = FrameLayout(this).apply { layoutDirection = View.LAYOUT_DIRECTION_RTL }
        mainContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
        
        topHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(16f)) }
        menuBtn = TextView(this).apply { text = "☰"; setTextColor(Color.WHITE); textSize = 28f; setPadding(0, 0, dp(16f), 0); setOnClickListener { feedback(it); openDrawer() } }
        titleText = TextView(this).apply { text = "מחשבון פרימיום"; setTextColor(Color.WHITE); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
        
        val pipBtn = TextView(this).apply { text = "🧮"; textSize = 22f; gravity = Gravity.CENTER; setPadding(dp(8f), dp(8f), dp(16f), dp(8f)); setOnClickListener { feedback(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(3, 4)).build()) } else { Toast.makeText(this@MainActivity, "חלון צף נתמך רק באנדרואיד 8 ומעלה", Toast.LENGTH_SHORT).show() }
        } }
        val historyBtn = TextView(this).apply { text = "🕒"; textSize = 20f; gravity = Gravity.CENTER; setPadding(dp(8f), dp(8f), dp(8f), dp(8f)); setOnClickListener { feedback(it); showHistoryDialog() } }

        topHeader.addView(menuBtn); topHeader.addView(titleText); topHeader.addView(pipBtn); topHeader.addView(historyBtn)
        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        mainContent?.addView(topHeader); mainContent?.addView(contentFrame)

        dimView = View(this).apply { setBackgroundColor(Color.parseColor("#B3000000")); visibility = View.GONE; alpha = 0f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); setOnClickListener { if (isDrawerOpen) closeDrawer() } }
        val drawerScroll = ScrollView(this).apply { layoutParams = FrameLayout.LayoutParams(dp(280f), FrameLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.RIGHT }; isClickable = true }
        drawerMenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(30f), 0, dp(30f)) }

        val appLogo = TextView(this).apply { text = "אולר שווייצרי\nPro fx-991"; setTextColor(Color.WHITE); textSize = 24f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(30f)) }
        drawerMenu?.addView(appLogo)

        for ((index, title) in categories.withIndex()) {
            val item = TextView(this).apply { text = title; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(24f), dp(16f), dp(24f), dp(16f)); setOnClickListener { feedback(it); switchTab(index); closeDrawer() } }
            drawerItemsViews.add(item); drawerMenu?.addView(item)
        }

        drawerScroll.addView(drawerMenu); rootFrame.addView(mainContent); rootFrame.addView(dimView); rootFrame.addView(drawerScroll)
        drawerScroll.translationX = dp(280f).toFloat(); setContentView(rootFrame); setupShortcuts(); handleIntent(intent)
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("he", "IL") }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) { super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig); if (isInPictureInPictureMode) { topHeader.visibility = View.GONE; if (isDrawerOpen) closeDrawer() } else { topHeader.visibility = View.VISIBLE } }
    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent?) { when (intent?.action) { "ACTION_CALC" -> { currentTabIndex = 0; applyTheme() }; "ACTION_SCI" -> { currentTabIndex = 1; applyTheme() }; "ACTION_COIN" -> { currentTabIndex = 13; applyTheme() }; else -> applyTheme() } }
    private fun setupShortcuts() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) { val sm = getSystemService(ShortcutManager::class.java); val s1 = ShortcutInfo.Builder(this, "coin").setShortLabel("מטבעות").setIcon(Icon.createWithResource(this, android.R.drawable.star_on)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_COIN" }).build(); val s2 = ShortcutInfo.Builder(this, "sci").setShortLabel("מדעית").setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_edit)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_SCI" }).build(); sm.dynamicShortcuts = listOf(s1, s2) } }

    private fun loadPreferences() {
        useHaptic = prefs?.getBoolean("useHaptic", true) ?: true; useSound = prefs?.getBoolean("useSound", true) ?: true; keepScreenOn = prefs?.getBoolean("keepScreenOn", false) ?: false; soundType = prefs?.getInt("soundType", 0) ?: 0
        calcPrecision = prefs?.getInt("calcPrecision", 8) ?: 8; buttonCornerStyle = prefs?.getFloat("buttonCornerStyle", 24f) ?: 24f
        primaryColor = prefs?.getInt("primaryColor", Color.parseColor("#64B5F6")) ?: Color.parseColor("#64B5F6"); bgColor = prefs?.getInt("bgColor", Color.parseColor("#08090C")) ?: Color.parseColor("#08090C")
        memoryValue = Double.fromBits(prefs?.getLong("memoryValue", 0L) ?: 0L); val savedHist = prefs?.getString("history", "") ?: ""
        fromCurrency = prefs?.getString("fromCurrency", "USD") ?: "USD"; toCurrency = prefs?.getString("toCurrency", "ILS") ?: "ILS"
        fromDist = prefs?.getString("fromDist", "קילומטר (km)") ?: "קילומטר (km)"; toDist = prefs?.getString("toDist", "מייל (mi)") ?: "מייל (mi)"
        fromWeight = prefs?.getString("fromWeight", "קילוגרם (kg)") ?: "קילוגרם (kg)"; toWeight = prefs?.getString("toWeight", "פאונד (lbs)") ?: "פאונד (lbs)"
        fromTemp = prefs?.getString("fromTemp", "צלזיוס (°C)") ?: "צלזיוס (°C)"; toTemp = prefs?.getString("toTemp", "פרנהייט (°F)") ?: "פרנהייט (°F)"
        historyList.clear(); if (savedHist.isNotEmpty()) historyList.addAll(savedHist.split("||")); updateKeepScreenOn()
    }

    fun savePreferences() { prefs?.edit()?.putBoolean("useHaptic", useHaptic)?.putBoolean("useSound", useSound)?.putBoolean("keepScreenOn", keepScreenOn)?.putInt("soundType", soundType)?.putInt("calcPrecision", calcPrecision)?.putFloat("buttonCornerStyle", buttonCornerStyle)?.putInt("primaryColor", primaryColor)?.putInt("bgColor", bgColor)?.putLong("memoryValue", memoryValue.toRawBits())?.putString("fromCurrency", fromCurrency)?.putString("toCurrency", toCurrency)?.putString("fromDist", fromDist)?.putString("toDist", toDist)?.putString("fromWeight", fromWeight)?.putString("toWeight", toWeight)?.putString("fromTemp", fromTemp)?.putString("toTemp", toTemp)?.putString("history", historyList.take(30).joinToString("||"))?.apply() }
    fun updateKeepScreenOn() { if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    fun applyTheme() { window.statusBarColor = bgColor; window.navigationBarColor = bgColor; mainContent?.setBackgroundColor(bgColor); drawerMenu?.setBackgroundColor(Utils.lightenColor(bgColor, 0.06f)); updateDrawerSelection(); switchTab(currentTabIndex) }
    private fun openDrawer() { val drawer = rootFrame.getChildAt(2); dimView.visibility = View.VISIBLE; dimView.animate().alpha(1f).setDuration(250).start(); drawer.animate().translationX(0f).setDuration(250).setInterpolator(android.view.animation.DecelerateInterpolator()).start(); isDrawerOpen = true }
    private fun closeDrawer() { val drawer = rootFrame.getChildAt(2); dimView.animate().alpha(0f).setDuration(250).withEndAction { dimView.visibility = View.GONE }.start(); drawer.animate().translationX(dp(280f).toFloat()).setDuration(250).setInterpolator(android.view.animation.AccelerateInterpolator()).start(); isDrawerOpen = false }
        fun feedback(view: View) { 
        if (useHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (useSound) { 
            if (soundType == 0) view.playSoundEffect(SoundEffectConstants.CLICK) 
            else if (soundType in 1..12 && soundIds[soundType] != 0) soundPool.play(soundIds[soundType], 1f, 1f, 0, 0, 1f)
        } 
    }

    fun copyToClipboard(text: String) { val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("CalcResult", text)); Toast.makeText(this, "הועתק ללוח", Toast.LENGTH_SHORT).show() }

    private fun showHistoryDialog() {
        if (historyList.isEmpty()) { Toast.makeText(this, "היסטוריית החישובים ריקה", Toast.LENGTH_SHORT).show(); return }
        val items = historyList.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("🕒 היסטוריה").setItems(items) { _, which -> val selected = items[which].substringAfter("=").trim(); currentInput = selected; justCalculated = true; if (currentTabIndex != 0 && currentTabIndex != 1) switchTab(0) else currentView?.text = currentInput }.setNeutralButton("נקה הכל") { _, _ -> historyList.clear(); savePreferences(); Toast.makeText(this, "נמחק", Toast.LENGTH_SHORT).show() }.setPositiveButton("סגור", null).show()
    }

    fun showItemPicker(title: String, items: List<String>, onSelected: (String) -> Unit) {
        val dialogView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16f), dp(16f), dp(16f), dp(16f)); setBackgroundColor(Utils.lightenColor(bgColor, 0.06f)) }
        val searchInput = EditText(this).apply { hint = "חפש..."; setHintTextColor(Color.parseColor("#5C657A")); setTextColor(Color.WHITE); setPadding(dp(12f), dp(12f), dp(12f), dp(12f)); background = GradientDrawable().apply { setColor(bgColor); cornerRadius = dp(8f).toFloat(); setStroke(dp(1f), Utils.lightenColor(bgColor, 0.15f)) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12f) } }
        val scrollView = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(400f)) }
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scrollView.addView(listContainer)
        val dialog = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle(title).setView(dialogView).create()
        fun populateList(query: String) {
            listContainer.removeAllViews(); val filtered = items.filter { it.contains(query, ignoreCase = true) }
            for (curr in filtered) {
                val itemBtn = TextView(this).apply { text = curr; setTextColor(Color.WHITE); textSize = 16f; setPadding(dp(16f), dp(16f), dp(16f), dp(16f)); setOnClickListener { feedback(it); onSelected(curr); dialog.dismiss() } }
                listContainer.addView(itemBtn); listContainer.addView(View(this).apply { setBackgroundColor(Utils.lightenColor(bgColor, 0.15f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)) })
            }
        }
        searchInput.addTextChangedListener(object : TextWatcher { override fun afterTextChanged(s: Editable?) { populateList(s.toString()) }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} })
        populateList(""); dialogView.addView(searchInput); dialogView.addView(scrollView); dialog.show()
    }

    private fun updateDrawerSelection() {
        for (i in drawerItemsViews.indices) {
            val tv = drawerItemsViews[i] as TextView
            if (i == currentTabIndex) { tv.setBackgroundColor(Color.argb(40, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))); tv.setTextColor(primaryColor) } 
            else { tv.setBackgroundColor(Color.TRANSPARENT); tv.setTextColor(Color.WHITE) }
        }
    }

    private fun switchTab(index: Int) {
        currentTabIndex = index
        titleText.text = categories[index].substringAfter(" ")
        updateDrawerSelection(); contentFrame.removeAllViews()
        val view = when (index) { 0 -> createBasicCalculator(); 1 -> createScientificCalculator(); 2 -> createVatCalculator(); 3 -> createLoanCalculator(); 4 -> createDistanceCalculator(); 5 -> createWeightCalculator(); 6 -> createTempCalculator(); 7 -> createProgrammerCalculator(); 8 -> createPercentageCalculator(); 9 -> createDiscountCalculator(); 10 -> createTipCalculator(); 11 -> createBMICalculator(); 12 -> createAgeCalculator(); 13 -> createCurrencyCalculator(); 14 -> createBmrCalculator(); 15 -> createQuadraticCalculator(); 16 -> createFractionCalculator(); 17 -> createSettingsPage(); else -> createBasicCalculator() }
        contentFrame.addView(view)
    }

    var currentInput = ""; var justCalculated = false
    var expressionView: TextView? = null; var currentView: TextView? = null; var memoryIndicator: TextView? = null

    private fun createBasicCalculator(): View = createCalculatorLayout(listOf(listOf("MC", "MR", "M+", "M-"), listOf("AC", "⌫", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("(", "0", ".", "=")))
    private fun createScientificCalculator(): View = createCalculatorLayout(listOf(listOf("sin", "cos", "tan", "log", "ln"), listOf("√", "x²", "x³", "x^y", "!"), listOf("π", "e", "(", ")", "÷"), listOf("7", "8", "9", "%", "×"), listOf("4", "5", "6", "MC", "−"), listOf("1", "2", "3", "MR", "+"), listOf("AC", "0", ".", "⌫", "=")), isGrid5x7 = true)

    private fun createCalculatorLayout(rows: List<List<String>>, isGrid5x7: Boolean = false): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(14f), dp(0f), dp(14f), dp(14f)) }
        val displayCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(18f), dp(14f), dp(18f), dp(14f)); background = GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(28f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) } }
        
        memoryIndicator = TextView(this).apply { text = if (memoryValue != 0.0) "M = ${Utils.formatResult(memoryValue, calcPrecision)}" else ""; setTextColor(primaryColor); textSize = 11f; gravity = Gravity.START }
        expressionView = TextView(this).apply { text = ""; setTextColor(Color.parseColor("#8A94A8")); setAutoSizeTextTypeUniformWithConfiguration(12, 22, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        currentView = TextView(this).apply { text = if(currentInput.isEmpty()) "0" else currentInput; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL); setAutoSizeTextTypeUniformWithConfiguration(22, 56, 2, TypedValue.COMPLEX_UNIT_SP); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f) }
        
        displayCard.addView(memoryIndicator); displayCard.addView(expressionView); displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.28f else 0.32f).apply { bottomMargin = dp(12f) })

        var startX = 0f; var startY = 0f
        displayCard.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startX; val dy = event.y - startY
                    if (Math.abs(dx) > 50f && Math.abs(dx) > Math.abs(dy)) {
                        onEquationButtonClick("⌫") 
                    } else if (dy > 50f && Math.abs(dy) > Math.abs(dx)) {
                        val textToRead = if(currentInput.isEmpty() || currentInput == "0") "אפס" else currentInput
                        tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)
                        Toast.makeText(this@MainActivity, "מקריא... 🔊", Toast.LENGTH_SHORT).show()
                    } else if (Math.abs(dx) < 10f && Math.abs(dy) < 10f) {
                        copyToClipboard(currentInput)
                    }
                    true
                }; else -> false
            }
        }
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
        val defaultBg = when { symbol == "AC" -> Utils.lightenColor(Color.RED, 0.2f); symbol == "=" -> primaryColor; isMem -> Utils.lightenColor(bgColor, 0.03f); symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> Utils.lightenColor(bgColor, 0.1f); else -> Utils.lightenColor(bgColor, 0.06f) }
        val strokeColor = if (symbol == "=" || symbol == "AC") Color.TRANSPARENT else Utils.lightenColor(bgColor, 0.15f)
        val textColor = when { symbol == "AC" -> Color.WHITE; symbol == "=" -> Color.parseColor("#08090C"); isMem || symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> primaryColor; else -> Color.WHITE }
        val shape = GradientDrawable().apply { this.shape = GradientDrawable.RECTANGLE; cornerRadius = dp(buttonCornerStyle).toFloat(); setColor(defaultBg); setStroke(dp(1.5f), strokeColor) }
        return TextView(this).apply {
            text = symbol; setTextColor(textColor); setAutoSizeTextTypeUniformWithConfiguration(10, if(isSmall) 22 else 28, 2, TypedValue.COMPLEX_UNIT_SP); typeface = if (symbol == "=" || isOp || isMem) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL); gravity = Gravity.CENTER; background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); isClickable = true
            setOnTouchListener { v, e -> when (e.actionMasked) { MotionEvent.ACTION_DOWN -> { feedback(v); v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).start() }; MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.animate().scaleX(1f).scaleY(1f).setDuration(150).start(); if (e.actionMasked == MotionEvent.ACTION_UP) onEquationButtonClick(symbol) } }; false }
        }
    }

    private fun onEquationButtonClick(s: String) {
        if (justCalculated && s !in listOf("÷", "×", "−", "+", "^", "%")) { currentInput = ""; justCalculated = false }
        justCalculated = false
        when (s) {
            "MC" -> { memoryValue = 0.0; savePreferences() }; "MR" -> { currentInput += Utils.formatResult(memoryValue, calcPrecision) }
            "M+" -> { try { memoryValue += Utils.evalMathExpression(if (currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch (e: Exception) {} }
            "M-" -> { try { memoryValue -= Utils.evalMathExpression(if (currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch (e: Exception) {} }
            "AC" -> { currentInput = ""; expressionView?.text = "" }; "⌫" -> { if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1) }
            "sin", "cos", "tan", "log", "ln" -> { currentInput += "$s(" }; "√" -> { currentInput += "√(" }; "x²" -> { currentInput += "^2" }; "x³" -> { currentInput += "^3" }; "x^y" -> { currentInput += "^" }
            "=" -> {
                if (checkEasterEggs(currentInput)) return
                
                if (currentInput.isNotEmpty()) {
                    try {
                        val res = Utils.evalMathExpression(currentInput)
                        val formatted = Utils.formatResult(res, calcPrecision)
                        expressionView?.text = "$currentInput ="
                        historyList.add(0, "$currentInput = $formatted")
                        savePreferences()
                        currentInput = formatted
                        justCalculated = true
                    } catch (e: Exception) { currentInput = "Error"; justCalculated = true }
                }
            }
            else -> currentInput += s
        }
        memoryIndicator?.text = if (memoryValue != 0.0) "M = ${Utils.formatResult(memoryValue, calcPrecision)}" else ""
        currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
    }

    fun createInputBox(hintText: String): EditText { return EditText(this).apply { hint = hintText; setHintTextColor(Color.parseColor("#5C657A")); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED; textSize = 17f; setPadding(dp(18f), dp(18f), dp(18f), dp(18f)); background = GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) } } }
    fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView { val shape = GradientDrawable().apply { setColor(if (isPrimary) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(buttonCornerStyle).toFloat(); if (!isPrimary) setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }; return TextView(this).apply { text = textStr; gravity = Gravity.CENTER; setTextColor(if (isPrimary) Color.parseColor("#08090C") else Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); setPadding(0, dp(16f), 0, dp(16f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }; isClickable = true; setOnClickListener { feedback(it); onClick(it) } } }
    fun createResultTextView(): TextView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(20f), 0, 0); setOnClickListener { if (text.isNotEmpty()) copyToClipboard(text.toString()) } }

    override fun onDestroy() { tts?.shutdown(); soundPool.release(); super.onDestroy() }
    fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
}
