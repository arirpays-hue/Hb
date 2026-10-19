package com.example.calculator

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sqrt

// --- מחשבונים משניים ---
fun MainActivity.createVatCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val amountInput = createInputBox("הכנס סכום (₪)")
    val vatInput = createInputBox("אחוז מע״מ (ברירת מחדל 17%)").apply { setText("17") }
    val resText = createResultTextView()
    val btnAdd = createActionBtn("הוסף מע״מ לסכום") { val a = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val v = vatInput.text.toString().toDoubleOrNull() ?: 17.0; val res = a * (1 + v / 100); resText.text = "סך הכל כולל מע״מ:\n${Utils.formatResult(res, 2)} ₪" }
    val btnSub = createActionBtn("חלץ מע״מ מהסכום", false) { val a = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val v = vatInput.text.toString().toDoubleOrNull() ?: 17.0; val res = a / (1 + v / 100); val vatAmount = a - res; resText.text = "סכום לפני מע״מ: ${Utils.formatResult(res, 2)} ₪\nסכום המע״מ: ${Utils.formatResult(vatAmount, 2)} ₪" }
    root.addView(amountInput); root.addView(vatInput); root.addView(btnAdd); root.addView(btnSub); root.addView(resText); return root
}

fun MainActivity.createLoanCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val amountInput = createInputBox("סכום ההלוואה (₪)")
    val monthsInput = createInputBox("תקופה (בחודשים)")
    val interestInput = createInputBox("ריבית שנתית (%)")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב החזר חודשי") {
        val p = amountInput.text.toString().toDoubleOrNull() ?: 0.0; val n = monthsInput.text.toString().toDoubleOrNull() ?: 0.0; val r = (interestInput.text.toString().toDoubleOrNull() ?: 0.0) / 100 / 12
        if (p > 0 && n > 0) { val res = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1); val total = res * n; resText.text = "החזר חודשי: ${Utils.formatResult(res, 2)} ₪\nסך הכל החזר: ${Utils.formatResult(total, 2)} ₪" }
    }
    root.addView(amountInput); root.addView(monthsInput); root.addView(interestInput); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createDistanceCalculator(): View { return createConverterLayout("מרחק", listOf("קילומטר (km)", "מייל (mi)", "מטר (m)", "סנטימטר (cm)", "רגל (ft)", "אינץ' (in)"), fromDist, toDist, { fromDist = it }, { toDist = it }) { v, f, t ->
    val inMeters = when (f) { "קילומטר (km)" -> v * 1000; "מייל (mi)" -> v * 1609.34; "סנטימטר (cm)" -> v / 100; "רגל (ft)" -> v * 0.3048; "אינץ' (in)" -> v * 0.0254; else -> v }
    when (t) { "קילומטר (km)" -> inMeters / 1000; "מייל (mi)" -> inMeters / 1609.34; "סנטימטר (cm)" -> inMeters * 100; "רגל (ft)" -> inMeters / 0.3048; "אינץ' (in)" -> inMeters / 0.0254; else -> inMeters }
} }

fun MainActivity.createWeightCalculator(): View { return createConverterLayout("משקל", listOf("קילוגרם (kg)", "פאונד (lbs)", "גרם (g)", "אונקיה (oz)", "טון (t)"), fromWeight, toWeight, { fromWeight = it }, { toWeight = it }) { v, f, t ->
    val inKg = when (f) { "פאונד (lbs)" -> v * 0.453592; "גרם (g)" -> v / 1000; "אונקיה (oz)" -> v * 0.0283495; "טון (t)" -> v * 1000; else -> v }
    when (t) { "פאונד (lbs)" -> inKg / 0.453592; "גרם (g)" -> inKg * 1000; "אונקיה (oz)" -> inKg / 0.0283495; "טון (t)" -> inKg / 1000; else -> inKg }
} }

fun MainActivity.createTempCalculator(): View { return createConverterLayout("טמפרטורה", listOf("צלזיוס (°C)", "פרנהייט (°F)", "קלווין (K)"), fromTemp, toTemp, { fromTemp = it }, { toTemp = it }) { v, f, t ->
    val inC = when (f) { "פרנהייט (°F)" -> (v - 32) * 5 / 9; "קלווין (K)" -> v - 273.15; else -> v }
    when (t) { "פרנהייט (°F)" -> inC * 9 / 5 + 32; "קלווין (K)" -> inC + 273.15; else -> inC }
} }

fun MainActivity.createConverterLayout(title: String, options: List<String>, currentFrom: String, currentTo: String, saveFrom: (String) -> Unit, saveTo: (String) -> Unit, calc: (Double, String, String) -> Double): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    var locFrom = currentFrom; var locTo = currentTo
    val btnFrom = createActionBtn("מ: $locFrom", false) {}
    val btnTo = createActionBtn("ל: $locTo", false) {}
    val input = createInputBox("הכנס ערך")
    val resText = createResultTextView()
    fun update() { savePreferences(); btnFrom.text = "מ: $locFrom"; btnTo.text = "ל: $locTo"; val v = input.text.toString().toDoubleOrNull(); if (v != null) resText.text = Utils.formatResult(calc(v, locFrom, locTo), 4) else resText.text = "" }
    btnFrom.setOnClickListener { feedback(it); showItemPicker("בחר מקור", options) { sel -> locFrom = sel; saveFrom(sel); update() } }
    btnTo.setOnClickListener { feedback(it); showItemPicker("בחר יעד", options) { sel -> locTo = sel; saveTo(sel); update() } }
    input.addTextChangedListener(object : TextWatcher { override fun afterTextChanged(s: Editable?) { update() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} })
    root.addView(btnFrom); root.addView(btnTo); root.addView(input); root.addView(resText); return root
}

fun MainActivity.createProgrammerCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val input = createInputBox("הכנס מספר עשרוני (Base 10)")
    val resText = TextView(this).apply { setTextColor(Color.WHITE); textSize = 18f; setPadding(0, dp(20f), 0, 0); setLineSpacing(dp(8f).toFloat(), 1.2f) }
    input.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            val v = s.toString().toLongOrNull()
            if (v != null) resText.text = "HEX (בסיס 16):\n${v.toString(16).uppercase()}\n\nOCT (בסיס 8):\n${v.toString(8)}\n\nBIN (בסיס 2):\n${v.toString(2)}" else resText.text = ""
        }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })
    root.addView(input); root.addView(resText); return root
}

fun MainActivity.createPercentageCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val val1 = createInputBox("חלק / אחוז"); val val2 = createInputBox("שלם / סכום"); val resText = createResultTextView()
    val btn1 = createActionBtn("כמה זה X% מתוך Y?") { val x = val1.text.toString().toDoubleOrNull() ?: 0.0; val y = val2.text.toString().toDoubleOrNull() ?: 0.0; resText.text = "תוצאה: ${Utils.formatResult(x * y / 100, 4)}" }
    val btn2 = createActionBtn("איזה אחוז זה X מתוך Y?", false) { val x = val1.text.toString().toDoubleOrNull() ?: 0.0; val y = val2.text.toString().toDoubleOrNull() ?: 0.0; if (y != 0.0) resText.text = "תוצאה: ${Utils.formatResult(x / y * 100, 4)}%" }
    root.addView(val1); root.addView(val2); root.addView(btn1); root.addView(btn2); root.addView(resText); return root
}

fun MainActivity.createDiscountCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val price = createInputBox("מחיר מקורי"); val discount = createInputBox("אחוז הנחה (%)"); val resText = createResultTextView()
    fun calc() { val p = price.text.toString().toDoubleOrNull() ?: 0.0; val d = discount.text.toString().toDoubleOrNull() ?: 0.0; val saved = p * (d / 100); resText.text = "מחיר סופי: ${Utils.formatResult(p - saved, 2)}\nנחסך: ${Utils.formatResult(saved, 2)}" }
    val tw = object : TextWatcher { override fun afterTextChanged(s: Editable?) { calc() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} }
    price.addTextChangedListener(tw); discount.addTextChangedListener(tw); root.addView(price); root.addView(discount); root.addView(resText); return root
}
fun MainActivity.createTipCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val bill = createInputBox("סכום החשבון"); val tip = createInputBox("אחוז טיפ (%)").apply { setText("10") }; val people = createInputBox("מספר סועדים").apply { setText("1") }; val resText = createResultTextView()
    fun calc() { val b = bill.text.toString().toDoubleOrNull() ?: 0.0; val t = tip.text.toString().toDoubleOrNull() ?: 10.0; val p = people.text.toString().toDoubleOrNull() ?: 1.0; if (p > 0) { val tipAmt = b * (t / 100); val total = b + tipAmt; resText.text = "טיפ סך הכל: ${Utils.formatResult(tipAmt, 2)}\nחשבון סופי: ${Utils.formatResult(total, 2)}\n\nלתשלום לכל סועד: ${Utils.formatResult(total / p, 2)}" } }
    val tw = object : TextWatcher { override fun afterTextChanged(s: Editable?) { calc() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} }
    bill.addTextChangedListener(tw); tip.addTextChangedListener(tw); people.addTextChangedListener(tw); root.addView(bill); root.addView(tip); root.addView(people); root.addView(resText); return root
}

fun MainActivity.createBMICalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val weight = createInputBox("משקל (ק״ג)"); val height = createInputBox("גובה (ס״מ)"); val resText = createResultTextView()
    val btn = createActionBtn("חשב BMI") { val w = weight.text.toString().toDoubleOrNull() ?: 0.0; val h = height.text.toString().toDoubleOrNull() ?: 0.0; if (h > 0) { val hMeters = h / 100; val bmi = w / (hMeters * hMeters); val status = when { bmi < 18.5 -> "תת משקל"; bmi < 25 -> "משקל תקין"; bmi < 30 -> "עודף משקל"; else -> "השמנת יתר" }; resText.text = "ה-BMI שלך: ${Utils.formatResult(bmi, 1)}\nמצב: $status" } }
    root.addView(weight); root.addView(height); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createAgeCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val btn = createActionBtn("בחר תאריך לידה") {}
    val resText = createResultTextView()
    btn.setOnClickListener {
        feedback(it); val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val dob = Calendar.getInstance().apply { set(y, m, d) }; val today = Calendar.getInstance()
            var ageYears = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) ageYears--
            val diff = today.timeInMillis - dob.timeInMillis; val days = diff / (1000 * 60 * 60 * 24)
            resText.text = "גיל בשנים: $ageYears\nגיל בימים: $days ימים"
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createCurrencyCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    var locFrom = fromCurrency; var locTo = toCurrency
    val btnFrom = createActionBtn("מ: $locFrom", false) {}
    val btnTo = createActionBtn("ל: $locTo", false) {}
    val input = createInputBox("סכום")
    val resText = createResultTextView()
    fun calc() {
        val v = input.text.toString().toDoubleOrNull(); if (v == null) { resText.text = ""; return }
        thread {
            try {
                val codeFrom = if (locFrom.length >= 3) locFrom.takeLast(4).trim('(', ')') else locFrom
                val codeTo = if (locTo.length >= 3) locTo.takeLast(4).trim('(', ')') else locTo
                val res = URL("https://open.er-api.com/v6/latest/$codeFrom").readText()
                val json = JSONObject(res); val rate = json.getJSONObject("rates").getDouble(codeTo)
                Handler(Looper.getMainLooper()).post { resText.text = "${Utils.formatResult(v * rate, 2)} $codeTo" }
            } catch (e: Exception) { Handler(Looper.getMainLooper()).post { resText.text = "שגיאת רשת" } }
        }
    }
    btnFrom.setOnClickListener { feedback(it); showItemPicker("מטבע מקור", allCurrencies) { sel -> locFrom = sel; fromCurrency = sel; savePreferences(); btnFrom.text = "מ: $locFrom"; calc() } }
    btnTo.setOnClickListener { feedback(it); showItemPicker("מטבע יעד", allCurrencies) { sel -> locTo = sel; toCurrency = sel; savePreferences(); btnTo.text = "ל: $locTo"; calc() } }
    input.addTextChangedListener(object : TextWatcher { override fun afterTextChanged(s: Editable?) { calc() }; override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {} })
    root.addView(btnFrom); root.addView(btnTo); root.addView(input); root.addView(resText); return root
}

fun MainActivity.createBmrCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    var isMale = true
    val genderBtn = createActionBtn("מין: גבר 👨", false) {}
    genderBtn.setOnClickListener { feedback(it); isMale = !isMale; genderBtn.text = if (isMale) "מין: גבר 👨" else "מין: אישה 👩" }
    val weight = createInputBox("משקל (ק״ג)"); val height = createInputBox("גובה (ס״מ)"); val age = createInputBox("גיל")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב BMR") {
        val w = weight.text.toString().toDoubleOrNull() ?: 0.0; val h = height.text.toString().toDoubleOrNull() ?: 0.0; val a = age.text.toString().toDoubleOrNull() ?: 0.0
        if (w > 0 && h > 0 && a > 0) { val bmr = if (isMale) 88.362 + (13.397 * w) + (4.799 * h) - (5.677 * a) else 447.593 + (9.247 * w) + (3.098 * h) - (4.330 * a); resText.text = "שריפת קלוריות במנוחה (BMR):\n${Utils.formatResult(bmr, 0)} קלוריות ליום" }
    }
    root.addView(genderBtn); root.addView(weight); root.addView(height); root.addView(age); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createQuadraticCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val aIn = createInputBox("מקדם a"); val bIn = createInputBox("מקדם b"); val cIn = createInputBox("מקדם c")
    val resText = createResultTextView()
    val btn = createActionBtn("חשב שורשים") {
        val a = aIn.text.toString().toDoubleOrNull() ?: 0.0; val b = bIn.text.toString().toDoubleOrNull() ?: 0.0; val c = cIn.text.toString().toDoubleOrNull() ?: 0.0
        if (a == 0.0) { resText.text = "לא משוואה ריבועית (a=0)" } else {
            val delta = b * b - 4 * a * c
            when { delta > 0 -> { val x1 = (-b + sqrt(delta)) / (2 * a); val x2 = (-b - sqrt(delta)) / (2 * a); resText.text = "X₁ = ${Utils.formatResult(x1, 4)}\nX₂ = ${Utils.formatResult(x2, 4)}" }; delta == 0.0 -> resText.text = "X = ${Utils.formatResult(-b / (2 * a), 4)}"; else -> resText.text = "אין פתרון ממשי (Δ < 0)" }
        }
    }
    root.addView(aIn); root.addView(bIn); root.addView(cIn); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createFractionCalculator(): View {
    val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(20f)) }
    val n1 = createInputBox("מונה 1"); val d1 = createInputBox("מכנה 1")
    val opBtn = createActionBtn("פעולה: חיבור (+)", false) {}
    var op = "+"
    opBtn.setOnClickListener { feedback(it); val ops = arrayOf("+", "-", "×", "÷"); val next = (ops.indexOf(op) + 1) % ops.size; op = ops[next]; opBtn.text = "פעולה: $op" }
    val n2 = createInputBox("מונה 2"); val d2 = createInputBox("מכנה 2"); val resText = createResultTextView()
    val btn = createActionBtn("חשב שבר") {
        val a = n1.text.toString().toDoubleOrNull() ?: 0.0; val b = d1.text.toString().toDoubleOrNull() ?: 1.0
        val c = n2.text.toString().toDoubleOrNull() ?: 0.0; val d = d2.text.toString().toDoubleOrNull() ?: 1.0
        if (b == 0.0 || d == 0.0) { resText.text = "חלוקה באפס!" } else {
            var rn = 0.0; var rd = 1.0
            when (op) { "+" -> { rn = a * d + c * b; rd = b * d }; "-" -> { rn = a * d - c * b; rd = b * d }; "×" -> { rn = a * c; rd = b * d }; "÷" -> { rn = a * d; rd = b * c } }
            resText.text = "${Utils.formatResult(rn, 0)} / ${Utils.formatResult(rd, 0)}\n= ${Utils.formatResult(rn / rd, 4)}"
        }
    }
    root.addView(n1); root.addView(d1); root.addView(opBtn); root.addView(n2); root.addView(d2); root.addView(btn); root.addView(resText); return root
}

fun MainActivity.createSettingsPage(): View {
    val scroll = ScrollView(this); val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(0f), dp(20f), dp(24f)) }
    
    val titleThemes = TextView(this).apply { text = "🎨 ערכות נושא מוכנות (Themes)"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)) }
    val themeScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(16f) } }
    val themeContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    val presetThemes = listOf(Triple("OLED", "#64B5F6", "#08090C"), Triple("Matrix", "#00E676", "#0A190F"), Triple("Barbie", "#FF4081", "#2A0A18"), Triple("Ocean", "#00B4D8", "#0A1128"), Triple("Sunset", "#FFB703", "#1A0F0A"), Triple("Dracula", "#BD93F9", "#282A36"), Triple("Cyber", "#FF007F", "#120458"))
    for (theme in presetThemes) { val tName = theme.first; val tPri = Color.parseColor(theme.second); val tBg = Color.parseColor(theme.third); val btn = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(12f), dp(12f), dp(12f), dp(12f)); background = GradientDrawable().apply { setColor(tBg); cornerRadius = dp(16f).toFloat(); setStroke(dp(2f), tPri) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(6f), dp(6f), dp(6f), dp(6f)) }; addView(TextView(this@createSettingsPage).apply { text = tName; setTextColor(tPri); textSize = 14f; typeface = Typeface.DEFAULT_BOLD }); setOnClickListener { feedback(it); primaryColor = tPri; bgColor = tBg; savePreferences(); applyTheme() } }; themeContainer.addView(btn) }
    themeScroll.addView(themeContainer)
    
    var editingTarget = 0; val targetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16f); bottomMargin = dp(14f) } }
    lateinit var btnEditPrimary: TextView; lateinit var btnEditBg: TextView; lateinit var pickerContainer: FrameLayout
    
    fun refreshTargetButtons() { 
        btnEditPrimary.background = GradientDrawable().apply { setColor(if (editingTarget == 0) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 0) Color.WHITE else Utils.lightenColor(bgColor, 0.15f)) }
        btnEditPrimary.setTextColor(if (editingTarget == 0) Color.parseColor("#08090C") else Color.WHITE)
        btnEditBg.background = GradientDrawable().apply { setColor(if (editingTarget == 1) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(14f).toFloat(); setStroke(dp(2f), if (editingTarget == 1) Color.WHITE else Utils.lightenColor(bgColor, 0.15f)) }
        btnEditBg.setTextColor(if (editingTarget == 1) Color.parseColor("#08090C") else Color.WHITE) 
    }
    
    // התיקון שלנו לצבעים החיים - קריאה לפונקציה מתוך ColorWheelView.kt
    fun loadColorPicker() { 
        refreshTargetButtons() 
        pickerContainer.removeAllViews() 
        pickerContainer.addView(createColorWheelPicker(this, if (editingTarget == 0) primaryColor else bgColor) { newColor -> 
            if (editingTarget == 0) {
                primaryColor = newColor
            } else {
                bgColor = newColor
                // עדכון חי וחלק של הרקע בלי לקרוא ל-applyTheme() כדי שהמסך לא יקפוץ למעלה
                window.statusBarColor = bgColor
                window.navigationBarColor = bgColor
                mainContent?.setBackgroundColor(bgColor)
                drawerMenu?.setBackgroundColor(Utils.lightenColor(bgColor, 0.06f))
            }
            savePreferences() 
            refreshTargetButtons() 
        }) 
    }
    
    btnEditPrimary = TextView(this).apply { text = "🎨 צבע נושא מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 0; loadColorPicker() } }
    btnEditBg = TextView(this).apply { text = "🌌 צבע רקע מותאם"; gravity = Gravity.CENTER; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(12f), 0, dp(12f)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(6f) }; setOnClickListener { feedback(it); editingTarget = 1; loadColorPicker() } }
    targetRow.addView(btnEditPrimary); targetRow.addView(btnEditBg); pickerContainer = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(18f) } }; loadColorPicker()
    
    val titleSettings = TextView(this).apply { text = "הגדרות מערכת נוספות"; setTextColor(Color.WHITE); textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setPadding(0, dp(8f), 0, dp(10f)) }
    
    val vibBtn = createActionBtn(if (useHaptic) "רטט בלחיצה: מופעל 🟢" else "רטט בלחיצה: כבוי 🔴", false) { useHaptic = !useHaptic; savePreferences(); applyTheme() }
    val sndBtn = createActionBtn(if (useSound) "צלילי הקשה: מופעל 🔊" else "צלילי הקשה: כבוי 🔇", false) { useSound = !useSound; savePreferences(); applyTheme() }
    
    val soundNames = arrayOf("מערכת 📱", "צליל 1 🎵", "צליל 2 🎶")
    val sndTypeBtn = createActionBtn("סוג צליל: ${soundNames.getOrElse(soundType) { "מערכת 📱" }}", false) { AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert).setTitle("בחר צליל").setSingleChoiceItems(soundNames, soundType) { d, w -> soundType = w; savePreferences(); applyTheme(); d.dismiss() }.show() }
    
    val scrBtn = createActionBtn(if (keepScreenOn) "מסך תמיד דולק: מופעל 🔆" else "מסך תמיד דולק: כבוי 🌙", false) { keepScreenOn = !keepScreenOn; updateKeepScreenOn(); savePreferences(); applyTheme() }
    val shapeName = when(buttonCornerStyle) { 8f -> "מרובעים ⏹️"; 24f -> "מודרניים 🔲"; else -> "עגולים ⚪" }
    val shapeBtn = createActionBtn("צורת כפתורים: $shapeName", false) { buttonCornerStyle = when(buttonCornerStyle) { 8f -> 24f; 24f -> 42f; else -> 8f }; savePreferences(); applyTheme() }
    val precBtn = createActionBtn("דיוק עשרוני: $calcPrecision ספרות ⚙️", false) { calcPrecision = if (calcPrecision >= 10) 2 else calcPrecision + 2; savePreferences(); applyTheme() }
    
    root.addView(titleThemes); root.addView(themeScroll); root.addView(targetRow); root.addView(pickerContainer); root.addView(titleSettings)
    root.addView(vibBtn); root.addView(sndBtn); root.addView(sndTypeBtn); root.addView(scrBtn); root.addView(shapeBtn); root.addView(precBtn); scroll.addView(root); return scroll
}
