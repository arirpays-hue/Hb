package com.example.calculator

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.Toast
import kotlin.math.*

object EasterEggs {

    fun handleCode(activity: MainActivity, code: String): Boolean {
        when (code.trim()) {
            "1337" -> {
                MiniGameEngine.startTicTacToe(activity)
                return true
            }
            "8080" -> {
                MiniGameEngine.startCalcInvaders(activity)
                return true
            }
            "5555" -> {
                MiniGameEngine.startMathRacer(activity)
                return true
            }
            "1010" -> {
                MiniGameEngine.startBombDefusal(activity)
                return true
            }
            "7777" -> {
                MiniGameEngine.startAnimalParty(activity)
                return true
            }
            "666" -> {
                activity.expressionView?.text = "😈 666 - Not today, Satan!"
                activity.currentView?.text = "🔥 😈 🔥"
                activity.primaryColor = Color.parseColor("#FF1744")
                activity.bgColor = Color.parseColor("#1A0000")
                activity.applyTheme()
                activity.shakeScreen()
                SoundEffectsEngine.playCreepyLaugh()
                Toast.makeText(activity, "😈 Not today, Satan!", Toast.LENGTH_SHORT).show()
                activity.justCalculated = true
                return true
            }
            "710", "0710", "7.10" -> {
                activity.memoryIndicator?.text = "🕯️ יזכור"
                activity.expressionView?.text = "לזכרם של נרצחי ונופלי ה-7 באוקטובר 🎗️"
                activity.currentView?.text = "עם ישראל חי 🇮🇱"
                activity.primaryColor = Color.parseColor("#FFD740")
                activity.bgColor = Color.parseColor("#05050A")
                activity.applyTheme()
                SoundEffectsEngine.playSadMemorialMelody()
                Toast.makeText(activity, "🕯️ לזכרם של הנופלים והנרצחים 🎗️", Toast.LENGTH_LONG).show()
                activity.justCalculated = true
                return true
            }
            "1999" -> {
                activity.expressionView?.text = "🕶️ THE MATRIX (1999)"
                activity.currentView?.text = "Wake up, Neo..."
                activity.primaryColor = Color.parseColor("#00FF41")
                activity.bgColor = Color.parseColor("#000000")
                activity.applyTheme()
                Toast.makeText(activity, "💊 Follow the white rabbit...", Toast.LENGTH_SHORT).show()
                activity.justCalculated = true
                return true
            }
            "1906" -> {
                activity.expressionView?.text = "🌍 סן פרנסיסקו (18 באפריל 1906)"
                activity.currentView?.text = "7.9 בסולם ריכטר!"
                activity.shakeScreen()
                Toast.makeText(activity, "🌋 רעידת האדמה הגדולה בסן פרנסיסקו!", Toast.LENGTH_SHORT).show()
                activity.justCalculated = true
                return true
            }
            "42" -> {
                activity.expressionView?.text = "🌌 התשובה לחיים, היקום וכל השאר"
                activity.currentView?.text = "42"
                Toast.makeText(activity, "🚀 Don't Panic!", Toast.LENGTH_SHORT).show()
                activity.justCalculated = true
                return true
            }
        }
        return false
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
    // 2 סוגי צלילי מקשים בלבד: 0 = צליל 1, 1 = צליל 2
    fun playKeyTone(style: Int) {
        Thread {
            try {
                val sampleRate = 22050
                val durationMs = 75
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val p = i.toDouble() / numSamples
                    val env = sin(p * PI)
                    val sample = if (style == 0) {
                        // צליל 1: טון נקי וחד
                        sin(2.0 * PI * 1200.0 * i / sampleRate) * env
                    } else {
                        // צליל 2: קליק עמוק ונמוך
                        sin(2.0 * PI * (340.0 - p * 180.0) * i / sampleRate) * (1.0 - p)
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
                val notes = listOf(440.0 to 480, 523.25 to 480, 493.88 to 480, 392.0 to 550, 329.63 to 680, 440.0 to 950)
                for ((freq, dur) in notes) {
                    playSingleNote(freq, dur); Thread.sleep(85)
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
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(buffer.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
        track.write(buffer, 0, buffer.size); track.play(); Thread.sleep(durationMs + 20); track.release()
    }
}
