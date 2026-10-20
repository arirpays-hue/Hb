package com.example.calculator

import android.app.AlertDialog
import android.graphics.Color
import android.text.InputType
import android.widget.*
import java.util.Calendar
import kotlin.math.*

object ExtraCalculators {

    fun showHubDialog(activity: MainActivity) {
        val options = arrayOf(
            "🏠 מחשבון משכנתא והלוואות",
            "🏷️ מחשבון הנחות ומע\"מ (18%)",
            "⛽ מחשבון צריכת דלק ונסיעה",
            "💻 המרת בסיסים (בינארי / הקסדצימלי)",
            "🎓 ממוצע ציונים משוקלל",
            "📐 מחשבון שטחים והיקפים (גיאומטריה)"
        )
        AlertDialog.Builder(activity)
            .setTitle("🧰 ארגז מחשבונים מתקדמים")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showLoanCalculator(activity)
                    1 -> showDiscountVatCalculator(activity)
                    2 -> showFuelCalculator(activity)
                    3 -> showBaseConverter(activity)
                    4 -> showGradeAverageCalculator(activity)
                    5 -> showGeometryCalculator(activity)
                }
            }
            .setNegativeButton("סגור", null)
            .show()
    }

    fun createInput(activity: MainActivity, hint: String, isText: Boolean = false): EditText {
        return EditText(activity).apply {
            this.hint = hint
            inputType = if (isText) InputType.TYPE_CLASS_TEXT else (InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
    }

    fun createBox(activity: MainActivity): Pair<LinearLayout, TextView> {
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 20)
            setBackgroundColor(Color.parseColor("#1A1A1A"))
        }
        val res = TextView(activity).apply {
            setTextColor(Color.GREEN)
            textSize = 18f
            setPadding(0, 24, 0, 0)
        }
        return layout to res
    }

    fun showLoanCalculator(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val amountInput = createInput(activity, "סכום ההלוואה (₪)")
        val rateInput = createInput(activity, "ריבית שנתית (%)")
        val yearsInput = createInput(activity, "תקופה בשנים")
        val calcBtn = Button(activity).apply {
            text = "חשב החזר חודשי"
            setOnClickListener {
                val p = amountInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val annualRate = rateInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val years = yearsInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val r = (annualRate / 100.0) / 12.0
                val n = years * 12.0
                val monthly = if (r == 0.0) p / n else p * (r * (1 + r).pow(n)) / ((1 + r).pow(n) - 1)
                val totalPay = monthly * n
                val totalInterest = totalPay - p
                resultView.text = "החזר חודשי: ₪%.2f\nסך הכל תשלומים: ₪%.2f\nסך ריבית: ₪%.2f".format(monthly, totalPay, totalInterest)
            }
        }
        layout.addView(amountInput); layout.addView(rateInput); layout.addView(yearsInput)
        layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("🏠 מחשבון משכנתא והלוואה").setView(layout).setPositiveButton("סגור", null).show()
    }
        fun showDiscountVatCalculator(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val priceInput = createInput(activity, "מחיר מקורי (₪)")
        val discountInput = createInput(activity, "אחוז הנחה (%) - למשל 20")
        val calcBtn = Button(activity).apply {
            text = "חשב הנחה ומע\"מ (18%)"
            setOnClickListener {
                val price = priceInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val disc = discountInput.text.toString().toDoubleOrNull() ?: 0.0
                val saved = price * (disc / 100.0)
                val finalPrice = price - saved
                val withoutVat = finalPrice / 1.18
                val vatAmount = finalPrice - withoutVat
                resultView.text = "מחיר סופי: ₪%.2f (חיסכון: ₪%.2f)\nלפני מע\"מ (18%%): ₪%.2f\nרכיב המע\"מ: ₪%.2f".format(finalPrice, saved, withoutVat, vatAmount)
            }
        }
        layout.addView(priceInput); layout.addView(discountInput); layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("🏷️ מחשבון הנחות ומע\"מ").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showFuelCalculator(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val distInput = createInput(activity, "מרחק נסיעה (ק\"מ)")
        val kmlInput = createInput(activity, "צריכת דלק (ק\"מ לליטר - למשל 14)")
        val priceInput = createInput(activity, "מחיר ליטר דלק (₪)")
        val passengersInput = createInput(activity, "מספר נוסעים לחלוקה (ברירת מחדל: 1)")
        val calcBtn = Button(activity).apply {
            text = "חשב עלות נסיעה"
            setOnClickListener {
                val dist = distInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val kml = kmlInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val price = priceInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val ppl = (passengersInput.text.toString().toIntOrNull() ?: 1).coerceAtLeast(1)
                val liters = dist / kml
                val totalCost = liters * price
                resultView.text = "דלק דרוש: %.2f ליטר\nעלות כוללת: ₪%.2f\nעלות לכל נוסע: ₪%.2f".format(liters, totalCost, totalCost / ppl)
            }
        }
        layout.addView(distInput); layout.addView(kmlInput); layout.addView(priceInput); layout.addView(passengersInput)
        layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("⛽ מחשבון דלק ונסיעה").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showBaseConverter(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val decInput = createInput(activity, "הכנס מספר עשרוני שלם")
        val calcBtn = Button(activity).apply {
            text = "המר בסיסים"
            setOnClickListener {
                val num = decInput.text.toString().toLongOrNull() ?: return@setOnClickListener
                resultView.text = "בינארי (BIN): ${java.lang.Long.toBinaryString(num)}\n" +
                        "הקסדצימלי (HEX): ${java.lang.Long.toHexString(num).uppercase()}\n" +
                        "אוקטלי (OCT): ${java.lang.Long.toOctalString(num)}"
            }
        }
        layout.addView(decInput); layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("💻 המרת בסיסים").setView(layout).setPositiveButton("סגור", null).show()
    }
        fun showGradeAverageCalculator(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val gradesInput = createInput(activity, "ציונים מופרדים בפסיק (90,85,100)", isText = true)
        val weightsInput = createInput(activity, "משקלים/יח\"ל בפסיק (5,4,3)", isText = true)
        val calcBtn = Button(activity).apply {
            text = "חשב ממוצע"
            setOnClickListener {
                val grades = gradesInput.text.toString().split(",").mapNotNull { it.trim().toDoubleOrNull() }
                val weights = weightsInput.text.toString().split(",").mapNotNull { it.trim().toDoubleOrNull() }
                if (grades.isEmpty()) return@setOnClickListener
                if (weights.size == grades.size && weights.sum() > 0) {
                    val avg = grades.indices.sumOf { grades[it] * weights[it] } / weights.sum()
                    resultView.text = "ממוצע משוקלל: %.2f\nממוצע רגיל: %.2f".format(avg, grades.average())
                } else {
                    resultView.text = "ממוצע רגיל: %.2f".format(grades.average())
                }
            }
        }
        layout.addView(gradesInput); layout.addView(weightsInput); layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("🎓 ממוצע ציונים משוקלל").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showGeometryCalculator(activity: MainActivity) {
        val (layout, resultView) = createBox(activity)
        val aInput = createInput(activity, "רדיוס / צלע א'")
        val bInput = createInput(activity, "צלע ב' / גובה (אופציונלי)")
        val calcBtn = Button(activity).apply {
            text = "חשב צורות"
            setOnClickListener {
                val a = aInput.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val b = bInput.text.toString().toDoubleOrNull() ?: a
                resultView.text = "⭕ מעגל (רדיוס $a): שטח=%.2f, היקף=%.2f\n".format(PI * a * a, 2 * PI * a) +
                        "⬛ מלבן ($a×$b): שטח=%.2f, היקף=%.2f\n".format(a * b, 2 * (a + b)) +
                        "📐 משולש ישר זווית: שטח=%.2f, יתר=%.2f".format((a * b) / 2.0, sqrt(a * a + b * b))
            }
        }
        layout.addView(aInput); layout.addView(bInput); layout.addView(calcBtn); layout.addView(resultView)
        AlertDialog.Builder(activity).setTitle("📐 מחשבון גיאומטריה").setView(layout).setPositiveButton("סגור", null).show()
    }
}

object BuiltInTools {

    fun showTipCalculator(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val bill = ExtraCalculators.createInput(activity, "סכום החשבון (₪)")
        val tipPct = ExtraCalculators.createInput(activity, "אחוז טיפ (למשל 12)")
        val people = ExtraCalculators.createInput(activity, "מספר סועדים")
        val btn = Button(activity).apply {
            text = "חשב טיפ"
            setOnClickListener {
                val b = bill.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val t = tipPct.text.toString().toDoubleOrNull() ?: 10.0
                val p = (people.text.toString().toIntOrNull() ?: 1).coerceAtLeast(1)
                val tipVal = b * (t / 100.0)
                val total = b + tipVal
                res.text = "טיפ: ₪%.2f\nסה\"כ לתשלום: ₪%.2f\nלכל סועד: ₪%.2f".format(tipVal, total, total / p)
            }
        }
        layout.addView(bill); layout.addView(tipPct); layout.addView(people); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("💰 חלוקת טיפים").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showBmiCalculator(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val weight = ExtraCalculators.createInput(activity, "משקל (ק\"ג)")
        val height = ExtraCalculators.createInput(activity, "גובה (ס\"מ)")
        val btn = Button(activity).apply {
            text = "חשב BMI"
            setOnClickListener {
                val w = weight.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val h = (height.text.toString().toDoubleOrNull() ?: return@setOnClickListener) / 100.0
                val bmi = w / (h * h)
                val status = when {
                    bmi < 18.5 -> "תת-משקל"
                    bmi < 25.0 -> "משקל תקין ✅"
                    bmi < 30.0 -> "עודף משקל"
                    else -> "השמנה"
                }
                res.text = "מדד BMI: %.1f\nקטגוריה: %s".format(bmi, status)
            }
        }
        layout.addView(weight); layout.addView(height); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("🏃 מדד BMI").setView(layout).setPositiveButton("סגור", null).show()
    }
        fun showAgeCalculator(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val yearIn = ExtraCalculators.createInput(activity, "שנת לידה (למשל 1995)")
        val btn = Button(activity).apply {
            text = "חשב גיל"
            setOnClickListener {
                val birthYear = yearIn.text.toString().toIntOrNull() ?: return@setOnClickListener
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val age = currentYear - birthYear
                res.text = "גילך: $age שנים\nבחודשים: ${age * 12}\nבימים (משוער): ${age * 365}"
            }
        }
        layout.addView(yearIn); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("🎂 חישוב גיל").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showCurrencyConverter(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val ils = ExtraCalculators.createInput(activity, "סכום בשקלים (₪)")
        val btn = Button(activity).apply {
            text = "המר מטבעות"
            setOnClickListener {
                val s = ils.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                res.text = "🇺🇸 דולר (USD): $%.2f\n🇪🇺 אירו (EUR): €%.2f\n🇬🇧 ליש\"ט (GBP): £%.2f".format(s / 3.65, s / 3.95, s / 4.65)
            }
        }
        layout.addView(ils); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("💵 מטבעות לייב").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showBmrCalculator(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val weight = ExtraCalculators.createInput(activity, "משקל (ק\"ג)")
        val height = ExtraCalculators.createInput(activity, "גובה (ס\"מ)")
        val age = ExtraCalculators.createInput(activity, "גיל")
        val btn = Button(activity).apply {
            text = "חשב שריפת קלוריות יומית (BMR)"
            setOnClickListener {
                val w = weight.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val h = height.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val a = age.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val bmr = 10 * w + 6.25 * h - 5 * a + 5
                res.text = "BMR בסיסי: %.0f קק\"ל ליום\nשמירה על משקל (פעילות קלה): %.0f קק\"ל".format(bmr, bmr * 1.375)
            }
        }
        layout.addView(weight); layout.addView(height); layout.addView(age); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("🔥 מחשבון BMR").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showQuadraticSolver(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val aIn = ExtraCalculators.createInput(activity, "מקדם a (של x²)")
        val bIn = ExtraCalculators.createInput(activity, "מקדם b (של x)")
        val cIn = ExtraCalculators.createInput(activity, "מקדם חופשי c")
        val btn = Button(activity).apply {
            text = "פתור משוואה (ax²+bx+c=0)"
            setOnClickListener {
                val a = aIn.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val b = bIn.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val c = cIn.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                val d = b * b - 4 * a * c
                res.text = when {
                    d < 0 -> "אין פתרון ממשי (Δ < 0)"
                    d == 0.0 -> "פתרון יחיד: x = %.3f".format(-b / (2 * a))
                    else -> "x₁ = %.3f\nx₂ = %.3f".format((-b + sqrt(d)) / (2 * a), (-b - sqrt(d)) / (2 * a))
                }
            }
        }
        layout.addView(aIn); layout.addView(bIn); layout.addView(cIn); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("➗ משוואות ריבועיות").setView(layout).setPositiveButton("סגור", null).show()
    }
        fun showFractionCalculator(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val n1 = ExtraCalculators.createInput(activity, "מונה שבר 1")
        val d1 = ExtraCalculators.createInput(activity, "מכנה שבר 1")
        val n2 = ExtraCalculators.createInput(activity, "מונה שבר 2")
        val d2 = ExtraCalculators.createInput(activity, "מכנה שבר 2")
        val btn = Button(activity).apply {
            text = "חבר וכפול שברים"
            setOnClickListener {
                val a = n1.text.toString().toIntOrNull() ?: return@setOnClickListener
                val b = d1.text.toString().toIntOrNull() ?: return@setOnClickListener
                val c = n2.text.toString().toIntOrNull() ?: return@setOnClickListener
                val d = d2.text.toString().toIntOrNull() ?: return@setOnClickListener
                if (b == 0 || d == 0) return@setOnClickListener
                val sumN = a * d + c * b
                val sumD = b * d
                val g = gcd(abs(sumN), abs(sumD))
                res.text = "חיבור: ${sumN / g}/${sumD / g} (עשרוני: %.3f)\nכפל: ${(a * c)}/${(b * d)}".format(sumN.toDouble() / sumD)
            }
        }
        layout.addView(n1); layout.addView(d1); layout.addView(n2); layout.addView(d2); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("½ מחשבון שברים").setView(layout).setPositiveButton("סגור", null).show()
    }

    private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

    fun showCompoundInterest(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val init = ExtraCalculators.createInput(activity, "סכום התחלתי (₪)")
        val monthly = ExtraCalculators.createInput(activity, "הפקדה חודשית (₪)")
        val rate = ExtraCalculators.createInput(activity, "תשואה שנתית (%)")
        val years = ExtraCalculators.createInput(activity, "מספר שנים")
        val btn = Button(activity).apply {
            text = "חשב ריבית דריבית"
            setOnClickListener {
                var total = init.text.toString().toDoubleOrNull() ?: 0.0
                val m = monthly.text.toString().toDoubleOrNull() ?: 0.0
                val r = ((rate.text.toString().toDoubleOrNull() ?: 5.0) / 100.0) / 12.0
                val months = ((years.text.toString().toIntOrNull() ?: 1) * 12)
                for (i in 1..months) total = (total + m) * (1 + r)
                val invested = (init.text.toString().toDoubleOrNull() ?: 0.0) + m * months
                res.text = "סכום סופי: ₪%.2f\nסה\"כ הופקד: ₪%.2f\nרווח מריבית: ₪%.2f".format(total, invested, total - invested)
            }
        }
        layout.addView(init); layout.addView(monthly); layout.addView(rate); layout.addView(years); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("📈 ריבית דריבית").setView(layout).setPositiveButton("סגור", null).show()
    }

    fun showSpeedConverter(activity: MainActivity) {
        val (layout, res) = ExtraCalculators.createBox(activity)
        val kmh = ExtraCalculators.createInput(activity, "מהירות בקמ\"ש (km/h)")
        val btn = Button(activity).apply {
            text = "המר מהירויות"
            setOnClickListener {
                val k = kmh.text.toString().toDoubleOrNull() ?: return@setOnClickListener
                res.text = "מייל לשעה (mph): %.2f\nמטר לשנייה (m/s): %.2f\nקשר ימי (Knots): %.2f".format(k * 0.621371, k / 3.6, k * 0.539957)
            }
        }
        layout.addView(kmh); layout.addView(btn); layout.addView(res)
        AlertDialog.Builder(activity).setTitle("🚀 המרת מהירות").setView(layout).setPositiveButton("סגור", null).show()
    }
}
