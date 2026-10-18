package com.example.calculator

import android.graphics.Color
import java.math.BigDecimal
import java.math.RoundingMode

object Utils {
    fun formatCurrency(value: Double): String = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    fun formatResult(res: Double, precision: Int): String {
        return try { if (res.isNaN() || res.isInfinite()) "Error" else BigDecimal.valueOf(res).setScale(precision, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() } catch (e: Exception) { "Error" }
    }

    fun lightenColor(color: Int, fraction: Float): Int {
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        return Color.rgb((r + (255 - r) * fraction).toInt(), (g + (255 - g) * fraction).toInt(), (b + (255 - b) * fraction).toInt())
    }

    fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

    fun evalMathExpression(str: String): Double {
        val expr = str.replace("×", "*").replace("÷", "/").replace("−", "-").replace("π", Math.PI.toString()).replace("e", Math.E.toString()).replace("%", "/100")
        return object : Any() {
            var pos = -1; var ch = 0
            fun nextChar() { ch = if (++pos < expr.length) expr[pos].toInt() else -1 }
            fun eat(charToEat: Int): Boolean { while (ch == ' '.toInt()) nextChar(); if (ch == charToEat) { nextChar(); return true }; return false }
            fun parse(): Double { nextChar(); val x = parseExpression(); if (pos < expr.length) throw RuntimeException("Unexpected"); return x }
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
                    x = when(func) { "sin" -> Math.sin(Math.toRadians(x)); "cos" -> Math.cos(Math.toRadians(x)); "tan" -> Math.tan(Math.toRadians(x)); "log" -> Math.log10(x); "ln" -> Math.log(x); "sqrt" -> Math.sqrt(x); else -> throw RuntimeException() }
                } else throw RuntimeException()
                if (eat('^'.toInt())) x = Math.pow(x, parseFactor())
                if (eat('!'.toInt())) { var fact = 1.0; for (i in 2..x.toInt()) fact *= i; x = fact }
                return x
            }
        }.parse()
    }
}
