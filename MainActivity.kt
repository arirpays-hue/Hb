package com.example.calculator

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    var mainContent: LinearLayout? = null
    var memoryIndicator: TextView? = null
    var expressionView: TextView? = null
    var currentView: TextView? = null
    val keypadButtons = mutableMapOf<String, Button>()

    private var rootFrame: FrameLayout? = null
    private var drawerOverlay: View? = null
    private var drawerPanel: ScrollView? = null
    private var isDrawerOpen = false

    var currentInput: String = ""
    private var fullExpression: String = ""
    var justCalculated: Boolean = false
    private var memoryValue: Double = 0.0
    private val historyList = mutableListOf<String>()

    private var isScientificMode = false
    private var isRightHanded = false
    var useHaptic = true
    private var useSound = true
    private var soundStyle = 0
    private var themeColor = Color.parseColor("#00E676")
    private var buttonCornerRadius = 60f

    private var settingsScrollY = 0
    private var isInSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreferences()
        buildMainScreen()
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        isRightHanded = prefs.getBoolean("isRightHanded", false)
        isScientificMode = prefs.getBoolean("isScientificMode", false)
        useHaptic = prefs.getBoolean("useHaptic", true)
        useSound = prefs.getBoolean("useSound", true)
        soundStyle = prefs.getInt("soundStyle", 0)
        themeColor = prefs.getInt("themeColor", Color.parseColor("#00E676"))
        buttonCornerRadius = prefs.getFloat("buttonCornerRadius", 60f)
    }

    private fun savePreferences() {
        getSharedPreferences("calc_prefs", Context.MODE_PRIVATE).edit().apply {
            putBoolean("isRightHanded", isRightHanded)
            putBoolean("isScientificMode", isScientificMode)
            putBoolean("useHaptic", useHaptic)
            putBoolean("useSound", useSound)
            putInt("soundStyle", soundStyle)
            putInt("themeColor", themeColor)
            putFloat("buttonCornerRadius", buttonCornerRadius)
            apply()
        }
    }

    private fun buildMainScreen() {
        isInSettings = false
        keypadButtons.clear()

        rootFrame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }

        mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 20)
            layoutDirection = if (isRightHanded) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        val menuBtn = TextView(this).apply {
            text = "☰"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(20, 8, 20, 8)
            setOnClickListener { animateKeyPress(this); toggleDrawer() }
        }

        val titleView = TextView(this).apply {
            text = if (isScientificMode) "מחשבון מדעי" else "מחשבון רגיל"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val miniCalcBtn = TextView(this).apply {
            text = "🧮"
            textSize = 24f
            setPadding(16, 8, 16, 8)
            setOnClickListener { animateKeyPress(this); showMiniCalculatorDialog() }
        }

        val historyBtn = TextView(this).apply {
            text = "🕒"
            textSize = 24f
            setPadding(16, 8, 16, 8)
            setOnClickListener { animateKeyPress(this); showHistoryDialog() }
        }

        topBar.addView(menuBtn)
        topBar.addView(titleView)
        topBar.addView(miniCalcBtn)
        topBar.addView(historyBtn)
        mainContent?.addView(topBar)

        buildDisplayAndKeypad()
        rootFrame?.addView(mainContent)
        buildDrawer()
        setContentView(rootFrame)
        applyTheme()
    }
        private fun buildDisplayAndKeypad() {
        val displayContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0D150D"))
                cornerRadius = 48f
                setStroke(3, adjustAlpha(themeColor, 130))
            }
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                if (isScientificMode) 1.15f else 1.35f
            )
            params.setMargins(0, 0, 0, 24)
            layoutParams = params
        }

        memoryIndicator = TextView(this).apply {
            text = if (memoryValue != 0.0) "M = ${formatNumber(memoryValue)}" else ""
            textSize = 14f
            setTextColor(themeColor)
            gravity = Gravity.END
        }

        expressionView = TextView(this).apply {
            text = fullExpression
            textSize = 20f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = Gravity.END
            textDirection = View.TEXT_DIRECTION_LTR
        }

        currentView = TextView(this).apply {
            text = if (currentInput.isEmpty()) "0" else currentInput
            textSize = 38f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            textDirection = View.TEXT_DIRECTION_LTR
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        displayContainer.addView(memoryIndicator)
        displayContainer.addView(expressionView)
        displayContainer.addView(currentView)
        mainContent?.addView(displayContainer)

        val keypadContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                if (isScientificMode) 2.85f else 2.45f
            )
        }

        val rows = mutableListOf<List<String>>()
        rows.add(listOf("MC", "MR", "M+", "M-"))
        if (isScientificMode) {
            rows.add(listOf("sin", "cos", "tan", "log"))
            rows.add(listOf("√", "x²", "^", "ln"))
            rows.add(listOf("π", "e", "!", ")"))
        }
        rows.add(listOf("AC", "⌫", "%", "÷"))
        rows.add(listOf("7", "8", "9", "×"))
        rows.add(listOf("4", "5", "6", "−"))
        rows.add(listOf("1", "2", "3", "+"))
        rows.add(listOf("(", "0", ".", "="))

        for (rowKeys in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                layoutDirection = if (isRightHanded) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            }
            for (key in rowKeys) {
                val weight = if (key == "0" && rowKeys.size == 4) 1.6f else 1f
                val btn = Button(this).apply {
                    text = key
                    isAllCaps = false
                    textSize = if (isScientificMode) 18f else 22f
                    setTypeface(null, Typeface.BOLD)
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
                    lp.setMargins(8, 6, 8, 6)
                    layoutParams = lp
                    setOnClickListener {
                        animateKeyPress(this)
                        handleKeyPress(key)
                    }
                }
                keypadButtons[key] = btn
                rowLayout.addView(btn)
            }
            keypadContainer.addView(rowLayout)
        }
        mainContent?.addView(keypadContainer)
    }

    private fun buildDrawer() {
        drawerOverlay = View(this).apply {
            setBackgroundColor(Color.parseColor("#99000000"))
            visibility = View.GONE
            setOnClickListener { closeDrawer() }
        }

        drawerPanel = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            visibility = View.GONE
            val widthPx = (resources.displayMetrics.widthPixels * 0.72).toInt()
            layoutParams = FrameLayout.LayoutParams(
                widthPx,
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (isRightHanded) Gravity.END else Gravity.START
            )
        }

        val menuList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 64)
        }

        val items = listOf(
            "מחשבון רגיל 🔢" to { switchCalcMode(false) },
            "מחשבון מדעי 🔬" to { switchCalcMode(true) },
            "חלוקת טיפים 💰" to { BuiltInTools.showTipCalculator(this) },
            "מדד BMI 🏃" to { BuiltInTools.showBmiCalculator(this) },
            "חישוב גיל 🎂" to { BuiltInTools.showAgeCalculator(this) },
            "מטבעות לייב 💵" to { BuiltInTools.showCurrencyConverter(this) },
            "מחשבון BMR 🔥" to { BuiltInTools.showBmrCalculator(this) },
            "משוואות ריבועיות ➗" to { BuiltInTools.showQuadraticSolver(this) },
            "מחשבון שברים ½" to { BuiltInTools.showFractionCalculator(this) },
            "ריבית דריבית 📈" to { BuiltInTools.showCompoundInterest(this) },
            "המרת מהירות 🚀" to { BuiltInTools.showSpeedConverter(this) },
            "ארגז מחשבונים מתקדמים 🧰" to { ExtraCalculators.showHubDialog(this) },
            "הגדרות ⚙️" to { showSettings() }
        )

        for ((label, action) in items) {
            val itemView = TextView(this).apply {
                text = label
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = if (isRightHanded) Gravity.END else Gravity.START
                setPadding(24, 30, 24, 30)
                setOnClickListener {
                    animateKeyPress(this)
                    closeDrawer()
                    action()
                }
            }
            menuList.addView(itemView)
        }

        drawerPanel?.addView(menuList)
        rootFrame?.addView(drawerOverlay)
        rootFrame?.addView(drawerPanel)
    }
    private fun toggleDrawer() {
        if (isDrawerOpen) closeDrawer() else openDrawer()
    }

    private fun openDrawer() {
        isDrawerOpen = true
        drawerOverlay?.visibility = View.VISIBLE
        drawerPanel?.visibility = View.VISIBLE
        val startX = if (isRightHanded) 600f else -600f
        drawerPanel?.translationX = startX
        drawerPanel?.animate()?.translationX(0f)?.setDuration(220)?.start()
    }

    private fun closeDrawer() {
        isDrawerOpen = false
        drawerOverlay?.visibility = View.GONE
        drawerPanel?.visibility = View.GONE
    }

    private fun switchCalcMode(scientific: Boolean) {
        if (MiniGameEngine.isActive) MiniGameEngine.stopGame(this)
        isScientificMode = scientific
        savePreferences()
        buildMainScreen()
    }

    fun applyTheme() {
        for ((key, btn) in keypadButtons) {
            val bg = GradientDrawable().apply {
                cornerRadius = buttonCornerRadius
                when (key) {
                    "AC" -> {
                        setColor(Color.parseColor("#FF3B30"))
                        setStroke(2, Color.parseColor("#FF6B60"))
                    }
                    "=" -> {
                        setColor(themeColor)
                        setStroke(2, Color.WHITE)
                    }
                    else -> {
                        setColor(Color.parseColor("#101410"))
                        setStroke(2, adjustAlpha(themeColor, 90))
                    }
                }
            }
            btn.background = bg

            when (key) {
                "AC" -> btn.setTextColor(Color.WHITE)
                "=" -> btn.setTextColor(Color.BLACK)
                in listOf("MC", "MR", "M+", "M-", "+", "−", "×", "÷", "%", "⌫") -> btn.setTextColor(themeColor)
                in listOf("sin", "cos", "tan", "log", "√", "x²", "^", "ln", "π", "e", "!") -> btn.setTextColor(adjustAlpha(themeColor, 210))
                else -> btn.setTextColor(Color.WHITE)
            }
        }
        applyHandednessLayout(isRightHanded)
    }

    fun applyHandednessLayout(rightHanded: Boolean) {
        currentView?.textDirection = View.TEXT_DIRECTION_LTR
        expressionView?.textDirection = View.TEXT_DIRECTION_LTR
    }

    fun animateKeyPress(view: View) {
        view.animate().scaleX(0.91f).scaleY(0.91f).setDuration(55).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(95).start()
        }.start()
    }

    fun shakeScreen() {
        mainContent?.let {
            ObjectAnimator.ofFloat(it, "translationX", 0f, 28f, -28f, 22f, -22f, 12f, -12f, 0f).apply {
                duration = 420
                start()
            }
        }
    }

    private fun handleKeyPress(key: String) {
        if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (useSound && !MiniGameEngine.isAnimalMode) SoundEffectsEngine.playKeyTone(soundStyle)

        if (MiniGameEngine.isActive) {
            MiniGameEngine.onKeyPress(this, key)
            return
        }

        when (key) {
            "AC" -> {
                currentInput = ""
                fullExpression = ""
                justCalculated = false
                updateDisplay()
            }
            "⌫" -> {
                if (currentInput.isNotEmpty()) {
                    currentInput = currentInput.dropLast(1)
                    updateDisplay()
                }
            }
            "MC" -> { memoryValue = 0.0; memoryIndicator?.text = "" }
            "MR" -> { currentInput = formatNumber(memoryValue); updateDisplay() }
            "M+" -> {
                memoryValue += (currentInput.toDoubleOrNull() ?: 0.0)
                memoryIndicator?.text = "M = ${formatNumber(memoryValue)}"
            }
            "M-" -> {
                memoryValue -= (currentInput.toDoubleOrNull() ?: 0.0)
                memoryIndicator?.text = "M = ${formatNumber(memoryValue)}"
            }
            "=" -> {
                if (checkEasterEggs(currentInput)) return
                evaluateCurrentExpression()
            }
            "sin", "cos", "tan", "log", "ln", "√" -> {
                if (justCalculated) { fullExpression = ""; justCalculated = false }
                currentInput += "$key("
                updateDisplay()
            }
            "x²" -> { currentInput += "^2"; updateDisplay() }
            "π" -> { currentInput += "3.14159265"; updateDisplay() }
            "e" -> { currentInput += "2.71828182"; updateDisplay() }
            "!" -> {
                val n = currentInput.toIntOrNull()
                if (n != null && n in 0..20) {
                    var res = 1L
                    for (i in 2..n) res *= i
                    fullExpression = "$n!"
                    currentInput = res.toString()
                    justCalculated = true
                    updateDisplay()
                }
            }
            "+", "−", "×", "÷", "%", "^" -> {
                if (justCalculated) justCalculated = false
                currentInput += key
                updateDisplay()
            }
            else -> {
                if (justCalculated) { currentInput = ""; fullExpression = ""; justCalculated = false }
                currentInput += key
                updateDisplay()
            }
        }
    }

    private fun checkEasterEggs(code: String): Boolean {
        when (code) {
            "1337" -> { MiniGameEngine.startTicTacToe(this); return true }
            "8080" -> { MiniGameEngine.startCalcInvaders(this); return true }
            "5555" -> { MiniGameEngine.startMathRacer(this); return true }
            "1010" -> { MiniGameEngine.startBombDefusal(this); return true }
            "7777" -> { MiniGameEngine.startAnimalParty(this); return true }
            "666" -> {
                expressionView?.text = "😈 666"
                currentView?.text = "Not today, Satan!"
                SoundEffectsEngine.playCreepyLaugh()
                shakeScreen()
                justCalculated = true
                return true
            }
            "710", "0710", "7.10" -> {
                memoryIndicator?.text = "🕯️ יזכור"
                expressionView?.text = "לזכרם של נרצחי ונופלי ה-7 באוקטובר 🎗️"
                currentView?.text = "עם ישראל חי 🇮🇱"
                SoundEffectsEngine.playSadMemorialMelody()
                justCalculated = true
                return true
            }
            "1999" -> {
                expressionView?.text = "🕶️ THE MATRIX (1999)"
                currentView?.text = "Wake up, Neo..."
                themeColor = Color.parseColor("#00FF41")
                applyTheme()
                justCalculated = true
                return true
            }
            "1906" -> {
                expressionView?.text = "🌍 סן פרנסיסקו (1906) - 7.9 בסולם ריכטר"
                currentView?.text = "רעידת אדמה!"
                shakeScreen()
                justCalculated = true
                return true
            }
            "42" -> {
                expressionView?.text = "🌌 התשובה לחיים, היקום וכל השאר"
                currentView?.text = "42"
                justCalculated = true
                return true
            }
        }
        return false
    }

    private fun evaluateCurrentExpression() {
        if (currentInput.isBlank()) return
        try {
            val expr = currentInput
            val result = MathEvaluator.eval(expr)
            val formatted = formatNumber(result)
            fullExpression = "$expr ="
            currentInput = formatted
            justCalculated = true
            historyList.add(0, "$expr = $formatted")
            updateDisplay()
        } catch (_: Exception) {
            expressionView?.text = "שגיאה בביטוי"
            shakeScreen()
        }
    }

    fun updateDisplay() {
        expressionView?.text = fullExpression
        currentView?.text = if (currentInput.isEmpty()) "0" else currentInput
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "שגיאה"
        val longVal = value.toLong()
        return if (abs(value - longVal) < 1e-9) longVal.toString()
        else "%.6f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
    }

    fun showSettings() {
        isInSettings = true
        val scroll = ScrollView(this).apply { setBackgroundColor(Color.parseColor("#0E0E0E")) }
        scroll.viewTreeObserver.addOnScrollChangedListener {
            if (isInSettings) settingsScrollY = scroll.scrollY
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 48, 40, 80)
        }

        val header = TextView(this).apply {
            text = "⚙️ הגדרות והתאמה אישית"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(themeColor)
            setPadding(0, 0, 0, 32)
        }
        container.addView(header)

        val handSwitch = Switch(this).apply {
            text = "✋ התאמה מלאה לימניים (תפריט, סרגל ומקלדת בימין)"
            textSize = 18f
            setTextColor(Color.WHITE)
            isChecked = isRightHanded
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, checked ->
                isRightHanded = checked
                savePreferences()
                showSettings()
            }
        }
        container.addView(handSwitch)

        val hapticSwitch = Switch(this).apply {
            text = "📳 רטט בלחיצה על מקשים"
            textSize = 18f
            setTextColor(Color.WHITE)
            isChecked = useHaptic
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, checked -> useHaptic = checked; savePreferences() }
        }
        container.addView(hapticSwitch)

        val soundSwitch = Switch(this).apply {
            text = "🔊 צלילי מקשים"
            textSize = 18f
            setTextColor(Color.WHITE)
            isChecked = useSound
            setPadding(0, 24, 0, 24)
            setOnCheckedChangeListener { _, checked -> useSound = checked; savePreferences() }
        }
        container.addView(soundSwitch)

        val soundNames = listOf(
            "1. דיגיטלי קריסטלי", "2. קליק מכני עץ", "3. לייזר עתידני",
            "4. פולס סאב-בס עמוק", "5. פעמון זכוכית", "6. רטרו ארקייד 8-ביט",
            "7. טיפת מים", "8. קפיץ מתכתי", "9. סינתיסייזר חם", "10. צפצוף הייטק קצר"
        )
        for (i in soundNames.indices) {
            val btn = Button(this).apply {
                text = if (soundStyle == i) "✅ ${soundNames[i]}" else soundNames[i]
                setTextColor(if (soundStyle == i) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (soundStyle == i) themeColor else Color.parseColor("#1E1E1E"))
                setOnClickListener {
                    soundStyle = i; savePreferences(); SoundEffectsEngine.playKeyTone(i); showSettings()
                }
            }
            container.addView(btn)
        }

        val colors = listOf(
            "#00E676" to "ירוק מטריקס", "#00B0FF" to "כחול ניאון",
            "#FF4081" to "ורוד סייבר", "#FFD740" to "זהב יוקרתי",
            "#E040FB" to "סגול גלקסי", "#FF6E40" to "כתום אש", "#00E5FF" to "טורקיז קרח"
        )
        for ((hex, name) in colors) {
            val c = Color.parseColor(hex)
            val colorBtn = Button(this).apply {
                text = if (themeColor == c) "✓ $name" else name
                setBackgroundColor(c)
                setTextColor(Color.BLACK)
                setOnClickListener { themeColor = c; savePreferences(); showSettings() }
            }
            container.addView(colorBtn)
        }

        val backBtn = Button(this).apply {
            text = "⬅️ חזרה למחשבון"
            textSize = 20f
            setBackgroundColor(themeColor)
            setTextColor(Color.BLACK)
            setOnClickListener { settingsScrollY = 0; buildMainScreen() }
        }
        container.addView(backBtn)

        scroll.addView(container)
        setContentView(scroll)
        scroll.post { scroll.scrollTo(0, settingsScrollY) }
    }

    private fun showHistoryDialog() {
        val items = if (historyList.isEmpty()) arrayOf("אין היסטוריית חישובים עדיין") else historyList.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("🕒 היסטוריית חישובים")
            .setItems(items) { _, which ->
                if (historyList.isNotEmpty()) {
                    currentInput = historyList[which].substringAfter("= ").trim()
                    updateDisplay()
                }
            }
            .setNeutralButton("נקה היסטוריה") { _, _ -> historyList.clear() }
            .setPositiveButton("סגור", null)
            .show()
    }

    private fun showMiniCalculatorDialog() {
        val input = EditText(this).apply {
            hint = "הקלד תרגיל מהיר (למשל: 25*4+10)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 20)
            setBackgroundColor(Color.parseColor("#1A1A1A"))
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("🧮 מיני מחשבון מהיר")
            .setView(layout)
            .setPositiveButton("חשב") { _, _ ->
                try {
                    val r = MathEvaluator.eval(input.text.toString())
                    Toast.makeText(this, "תוצאה: ${formatNumber(r)}", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "תרגיל לא תקין", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("סגור", null)
            .show()
    }

    private fun adjustAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}

object MathEvaluator {
    fun eval(str: String): Double {
        val clean = str.replace("×", "*").replace("÷", "/").replace("−", "-")
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < clean.length) clean[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < clean.length) throw RuntimeException("Unexpected")
                return x
            }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        eat('%'.code) -> x %= parseFactor()
                        else -> return x
                    }
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, pos).toDouble()
                } else if (ch in 'a'.code..'z'.code || ch == '√'.code) {
                    while (ch in 'a'.code..'z'.code || ch == '√'.code) nextChar()
                    val func = clean.substring(startPos, pos)
                    x = parseFactor()
                    x = when (func) {
                        "√" -> sqrt(x)
                        "sin" -> sin(Math.toRadians(x))
                        "cos" -> cos(Math.toRadians(x))
                        "tan" -> tan(Math.toRadians(x))
                        "log" -> log10(x)
                        "ln" -> ln(x)
                        else -> throw RuntimeException("Unknown")
                    }
                } else throw RuntimeException("Unexpected")
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }.parse()
    }
}

object SoundEffectsEngine {
    fun playKeyTone(style: Int) {
        Thread {
            try {
                val sampleRate = 22050
                val durationMs = 85
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val p = i.toDouble() / numSamples
                    val env = sin(p * PI)
                    val sample = when (style) {
                        0 -> sin(2.0 * PI * 1400.0 * i / sampleRate) * env
                        1 -> sin(2.0 * PI * (320.0 - p * 180.0) * i / sampleRate) * (1.0 - p)
                        2 -> sin(2.0 * PI * (600.0 + p * 1800.0) * i / sampleRate) * env
                        3 -> sin(2.0 * PI * 140.0 * i / sampleRate) * (1.0 - p)
                        4 -> (sin(2.0 * PI * 2100.0 * i / sampleRate) * 0.7 + sin(2.0 * PI * 4200.0 * i / sampleRate) * 0.3) * (1.0 - p)
                        5 -> (if (sin(2.0 * PI * 880.0 * i / sampleRate) >= 0) 0.6 else -0.6) * env
                        6 -> sin(2.0 * PI * (900.0 + sin(p * 12.0) * 300.0) * i / sampleRate) * env
                        7 -> sin(2.0 * PI * (450.0 + (i % 40) * 15.0) * i / sampleRate) * env
                        8 -> (sin(2.0 * PI * 520.0 * i / sampleRate) + sin(2.0 * PI * 780.0 * i / sampleRate)) * 0.5 * env
                        else -> sin(2.0 * PI * 1850.0 * i / sampleRate) * (1.0 - p)
                    }
                    buffer[i] = (sample * Short.MAX_VALUE * 0.45).toInt().toShort()
                }
                playPcmBuffer(buffer, sampleRate, durationMs.toLong())
            } catch (_: Exception) {}
        }.start()
    }

    fun playCreepyLaugh() {
        Thread {
            try {
                for (f in listOf(440.0, 392.0, 349.0, 311.0, 277.0)) {
                    playSingleNote(f, 130); Thread.sleep(40)
                }
            } catch (_: Exception) {}
        }.start()
    }

    fun playSadMemorialMelody() {
        Thread {
            try {
                val notes = listOf(440.0 to 450, 523.25 to 450, 493.88 to 450, 392.0 to 550, 329.63 to 700, 440.0 to 900)
                for ((freq, dur) in notes) {
                    playSingleNote(freq, dur); Thread.sleep(80)
                }
            } catch (_: Exception) {}
        }.start()
    }

    private fun playSingleNote(freq: Double, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val env = sin((i.toDouble() / numSamples) * PI)
            buffer[i] = (sin(2.0 * PI * freq * i / sampleRate) * env * Short.MAX_VALUE * 0.4).toInt().toShort()
        }
        playPcmBuffer(buffer, sampleRate, durationMs.toLong())
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int, durationMs: Long) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs + 20)
        track.release()
    }
}

    
