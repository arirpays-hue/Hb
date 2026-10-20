package com.example.calculator

import android.animation.ObjectAnimator
import android.app.Activity
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
import kotlin.math.*

class MainActivity : Activity() {

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
    var fullExpression: String = ""
    var justCalculated: Boolean = false
    var memoryValue: Double = 0.0
    val historyList = mutableListOf<String>()

    // משתנים גלויים לכל הקבצים בפרויקט (מונע שגיאות קומפילציה)
    var isScientificMode: Boolean = false
    var isRightHanded: Boolean = false
    var menuRightSide: Boolean = false
    var useHaptic: Boolean = true
    var useSound: Boolean = true
    var soundStyle: Int = 0
    var primaryColor: Int = Color.parseColor("#00E676")
    var themeColor: Int
        get() = primaryColor
        set(value) { primaryColor = value }
    var bgColor: Int = Color.parseColor("#000000")
    var buttonCornerRadius: Float = 60f
    var calcPrecision: Int = 6

    private var settingsScrollY = 0
    private var isInSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadPreferences()
        buildMainScreen()
    }

    fun loadPreferences() {
        val prefs = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
        isRightHanded = prefs.getBoolean("isRightHanded", false)
        menuRightSide = isRightHanded
        isScientificMode = prefs.getBoolean("isScientificMode", false)
        useHaptic = prefs.getBoolean("useHaptic", true)
        useSound = prefs.getBoolean("useSound", true)
        soundStyle = prefs.getInt("soundStyle", 0)
        primaryColor = prefs.getInt("primaryColor", Color.parseColor("#00E676"))
        bgColor = prefs.getInt("bgColor", Color.parseColor("#000000"))
        buttonCornerRadius = prefs.getFloat("buttonCornerRadius", 60f)
        calcPrecision = prefs.getInt("calcPrecision", 6)
    }

    fun savePreferences() {
        menuRightSide = isRightHanded
        val editor = getSharedPreferences("calc_prefs", Context.MODE_PRIVATE).edit()
        editor.putBoolean("isRightHanded", isRightHanded)
        editor.putBoolean("isScientificMode", isScientificMode)
        editor.putBoolean("useHaptic", useHaptic)
        editor.putBoolean("useSound", useSound)
        editor.putInt("soundStyle", soundStyle)
        editor.putInt("primaryColor", primaryColor)
        editor.putInt("bgColor", bgColor)
        editor.putFloat("buttonCornerRadius", buttonCornerRadius)
        editor.putInt("calcPrecision", calcPrecision)
        editor.apply()
    }
        fun buildMainScreen() {
        isInSettings = false
        keypadButtons.clear()

        rootFrame = FrameLayout(this).apply { setBackgroundColor(bgColor) }

        mainContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(bgColor)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // סרגל עליון שמתהפך אוטומטית בין ימניים לשמאליים
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

        val displayContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 28, 36, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0D150D"))
                cornerRadius = 48f
                setStroke(3, adjustAlpha(primaryColor, 130))
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
            setTextColor(primaryColor)
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
                // התאמת כפתורי המקלדת (כולל AC והפעולות) לימניים או שמאליים
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
        rootFrame?.addView(mainContent)
        buildDrawer()
        setContentView(rootFrame)
        applyTheme()
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
            val widthPx = (resources.displayMetrics.widthPixels * 0.74).toInt()
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
            "חלוקת טיפים 💰" to { SecondaryCalculators.showTipCalculator(this) },
            "מדד BMI 🏃" to { SecondaryCalculators.showBmiCalculator(this) },
            "חישוב גיל 🎂" to { SecondaryCalculators.showAgeCalculator(this) },
            "מטבעות לייב 💵" to { SecondaryCalculators.showCurrencyConverter(this) },
            "מחשבון BMR 🔥" to { SecondaryCalculators.showBmrCalculator(this) },
            "משוואות ריבועיות ➗" to { SecondaryCalculators.showQuadraticSolver(this) },
            "מחשבון שברים ½" to { SecondaryCalculators.showFractionCalculator(this) },
            "ריבית דריבית 📈" to { SecondaryCalculators.showCompoundInterest(this) },
            "המרת מהירות 🚀" to { SecondaryCalculators.showSpeedConverter(this) },
            "משכנתא והלוואות 🏠" to { SecondaryCalculators.showLoanCalculator(this) },
            "הנחות ומע\"מ 🏷️" to { SecondaryCalculators.showDiscountVatCalculator(this) },
            "צריכת דלק ונסיעה ⛽" to { SecondaryCalculators.showFuelCalculator(this) },
            "המרת בסיסים (BIN/HEX) 💻" to { SecondaryCalculators.showBaseConverter(this) },
            "ממוצע ציונים משוקלל 🎓" to { SecondaryCalculators.showGradeAverageCalculator(this) },
            "גיאומטריה (שטחים) 📐" to { SecondaryCalculators.showGeometryCalculator(this) },
            "הגדרות ⚙️" to { showSettings() }
        )

        for ((label, action) in items) {
            val itemView = TextView(this).apply {
                text = label
                textSize = 19f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = if (isRightHanded) Gravity.END else Gravity.START
                setPadding(24, 28, 24, 28)
                setOnClickListener { animateKeyPress(this); closeDrawer(); action() }
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
        mainContent?.setBackgroundColor(bgColor)
        rootFrame?.setBackgroundColor(bgColor)
        for ((key, btn) in keypadButtons) {
            val bg = GradientDrawable().apply {
                cornerRadius = buttonCornerRadius
                when (key) {
                    "AC" -> {
                        setColor(Color.parseColor("#FF3B30"))
                        setStroke(2, Color.parseColor("#FF6B60"))
                    }
                    "=" -> {
                        setColor(primaryColor)
                        setStroke(2, Color.WHITE)
                    }
                    else -> {
                        setColor(Color.parseColor("#101410"))
                        setStroke(2, adjustAlpha(primaryColor, 95))
                    }
                }
            }
            btn.background = bg
            when (key) {
                "AC" -> btn.setTextColor(Color.WHITE)
                "=" -> btn.setTextColor(Color.BLACK)
                in listOf("MC", "MR", "M+", "M-", "+", "−", "×", "÷", "%", "⌫") -> btn.setTextColor(primaryColor)
                in listOf("sin", "cos", "tan", "log", "√", "x²", "^", "ln", "π", "e", "!") -> btn.setTextColor(adjustAlpha(primaryColor, 215))
                else -> btn.setTextColor(Color.WHITE)
            }
        }
        currentView?.textDirection = View.TEXT_DIRECTION_LTR
        expressionView?.textDirection = View.TEXT_DIRECTION_LTR
    }

    fun animateKeyPress(view: View) {
        view.animate().scaleX(0.90f).scaleY(0.90f).setDuration(50).withEndAction {
            view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(90).start()
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
            "AC" -> { currentInput = ""; fullExpression = ""; justCalculated = false; updateDisplay() }
            "⌫" -> { if (currentInput.isNotEmpty()) { currentInput = currentInput.dropLast(1); updateDisplay() } }
            "MC" -> { memoryValue = 0.0; memoryIndicator?.text = "" }
            "MR" -> { currentInput = formatNumber(memoryValue); updateDisplay() }
            "M+" -> { memoryValue += (currentInput.toDoubleOrNull() ?: 0.0); memoryIndicator?.text = "M = ${formatNumber(memoryValue)}" }
            "M-" -> { memoryValue -= (currentInput.toDoubleOrNull() ?: 0.0); memoryIndicator?.text = "M = ${formatNumber(memoryValue)}" }
            "=" -> {
                if (EasterEggs.handleCode(this, currentInput)) return
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
        else "%.${calcPrecision}f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
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

        container.addView(TextView(this).apply {
            text = "⚙️ הגדרות והתאמה אישית"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(primaryColor)
            setPadding(0, 0, 0, 28)
        })

        container.addView(Switch(this).apply {
            text = "✋ התאמה מלאה לימניים (תפריט, סרגל ומקלדת בימין)"
            textSize = 17f; setTextColor(Color.WHITE); isChecked = isRightHanded; setPadding(0, 20, 0, 20)
            setOnCheckedChangeListener { _, checked -> isRightHanded = checked; savePreferences(); showSettings() }
        })

        container.addView(Switch(this).apply {
            text = "📳 רטט בלחיצה על מקשים"
            textSize = 17f; setTextColor(Color.WHITE); isChecked = useHaptic; setPadding(0, 20, 0, 20)
            setOnCheckedChangeListener { _, checked -> useHaptic = checked; savePreferences() }
        })

        container.addView(Switch(this).apply {
            text = "🔊 צלילי מקשים"
            textSize = 17f; setTextColor(Color.WHITE); isChecked = useSound; setPadding(0, 20, 0, 20)
            setOnCheckedChangeListener { _, checked -> useSound = checked; savePreferences() }
        })

        // שני כפתורי צליל בלבד: צליל 1 וצליל 2
        val soundRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 24)
        }
        val soundNames = listOf("צליל 1", "צליל 2")
        for (i in 0..1) {
            soundRow.addView(Button(this).apply {
                text = if (soundStyle == i) "✅ ${soundNames[i]}" else soundNames[i]
                setTextColor(if (soundStyle == i) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (soundStyle == i) primaryColor else Color.parseColor("#1E1E1E"))
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                lp.setMargins(8, 0, 8, 0)
                layoutParams = lp
                setOnClickListener { soundStyle = i; savePreferences(); SoundEffectsEngine.playKeyTone(i); showSettings() }
            })
        }
        container.addView(soundRow)

        val colors = listOf(
            "#00E676" to "ירוק מטריקס", "#00B0FF" to "כחול ניאון", "#FF4081" to "ורוד סייבר",
            "#FFD740" to "זהב יוקרתי", "#E040FB" to "סגול גלקסי", "#FF6E40" to "כתום אש", "#00E5FF" to "טורקיז קרח"
        )
        for ((hex, name) in colors) {
            val c = Color.parseColor(hex)
            container.addView(Button(this).apply {
                text = if (primaryColor == c) "✓ צבע: $name" else "צבע: $name"
                setBackgroundColor(c); setTextColor(Color.BLACK)
                setOnClickListener { primaryColor = c; savePreferences(); showSettings() }
            })
        }

        container.addView(Button(this).apply {
            text = "⬅️ חזרה למחשבון"
            textSize = 20f; setBackgroundColor(primaryColor); setTextColor(Color.BLACK)
            setOnClickListener { settingsScrollY = 0; buildMainScreen() }
        })

        scroll.addView(container)
        setContentView(scroll)
        scroll.post { scroll.scrollTo(0, settingsScrollY) }
    }

    private fun showHistoryDialog() {
        val items = if (historyList.isEmpty()) arrayOf("אין היסטוריית חישובים עדיין") else historyList.toTypedArray()
        AlertDialog.Builder(this).setTitle("🕒 היסטוריית חישובים").setItems(items) { _, which ->
            if (historyList.isNotEmpty()) { currentInput = historyList[which].substringAfter("= ").trim(); updateDisplay() }
        }.setNeutralButton("נקה") { _, _ -> historyList.clear() }.setPositiveButton("סגור", null).show()
    }

    private fun showMiniCalculatorDialog() {
        val input = EditText(this).apply { hint = "הקלד תרגיל מהיר (25*4+10)"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        AlertDialog.Builder(this).setTitle("🧮 מיני מחשבון").setView(input).setPositiveButton("חשב") { _, _ ->
            try { Toast.makeText(this, "תוצאה: ${formatNumber(MathEvaluator.eval(input.text.toString()))}", Toast.LENGTH_LONG).show() }
            catch (_: Exception) { Toast.makeText(this, "תרגיל לא תקין", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("סגור", null).show()
    }

    private fun adjustAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
