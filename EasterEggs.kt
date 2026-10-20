package com.example.calculator

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

var activeMediaPlayer: MediaPlayer? = null

object MemorialSound {
    fun playSadMemorialMelody() {
        thread {
            try {
                val sampleRate = 22050
                val notes = listOf(440.0 to 480, 523.25 to 480, 493.88 to 480, 392.0 to 550, 329.63 to 680, 440.0 to 950)
                for ((freq, dur) in notes) {
                    val numSamples = (dur * sampleRate) / 1000
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val env = sin((i.toDouble() / numSamples) * PI)
                        buffer[i] = (sin(2.0 * PI * freq * i / sampleRate) * env * Short.MAX_VALUE * 0.45).toInt().toShort()
                    }
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                        .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                        .setBufferSizeInBytes(buffer.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                    track.write(buffer, 0, buffer.size); track.play(); Thread.sleep(dur.toLong() + 25); track.release()
                }
            } catch (_: Exception) {}
        }
    }
}

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        // --- 🎮 מיני-משחקים בתוך המחשבון ---
        "1337" -> {
            MiniGameEngine.startTicTacToe(this)
            return true
        }
        "8080" -> {
            MiniGameEngine.startCalcInvaders(this)
            return true
        }
        "5555" -> {
            MiniGameEngine.startMathRacer(this)
            return true
        }
        "1010" -> {
            MiniGameEngine.startBombDefusal(this)
            return true
        }
        "7777" -> {
            MiniGameEngine.startAnimalParty(this)
            return true
        }

        // --- ביצי סאונד מקוריות ---
        "123456789" -> {
            justCalculated = true
            val resId = resources.getIdentifier("cheer", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ קובץ cheer.mp3 לא נמצא באפליקציה!", Toast.LENGTH_SHORT).show()
            }
            val orig = bgColor
            thread {
                val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN)
                for (i in 1..15) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors.random()) }; Thread.sleep(100) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }
                "911" -> {
            justCalculated = true
            val resId = resources.getIdentifier("siren", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            } else {
                Toast.makeText(this, "⚠️ הסירנה חסרה!", Toast.LENGTH_LONG).show()
            }
            val orig = bgColor
            thread {
                val colors = listOf(Color.RED, Color.BLUE)
                for (i in 1..20) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(150) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }

        // 🎗️ מחווה ל-7 באוקטובר עם מנגינה שקטה ועצובה
        "0710", "710" -> {
            primaryColor = Color.parseColor("#FFD700")
            savePreferences()
            applyTheme()
            expressionView?.text = "07/10/2023"
            currentInput = "🎗️ לא נשכח ולא נסלח 🎗️"
            currentView?.text = currentInput
            justCalculated = true
            MemorialSound.playSadMemorialMelody()
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        // 💻 מצב מטריקס
        "1999" -> {
            primaryColor = Color.parseColor("#00FF00")
            bgColor = Color.parseColor("#000000")
            savePreferences()
            applyTheme()
            expressionView?.text = "Matrix"
            currentInput = "Wake up, Neo..."
            currentView?.text = currentInput
            justCalculated = true
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return true
        }

        // 🌍 רעידת האדמה בסן פרנסיסקו
        "1906" -> {
            expressionView?.text = "San Francisco"
            currentInput = "Earthquake! 🌍"
            currentView?.text = currentInput
            justCalculated = true
            shakeScreen()
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        // 🌌 התשובה לחיים, ליקום וכל השאר
        "42" -> {
            expressionView?.text = "The Answer to Life"
            currentInput = "42"
            currentView?.text = currentInput
            justCalculated = true
            Toast.makeText(this, "התשובה ליקום היא 42", Toast.LENGTH_SHORT).show()
            return true
        }

        // 😈 שטן
        "666" -> {
            primaryColor = Color.parseColor("#FF0000")
            savePreferences()
            applyTheme()
            expressionView?.text = "Hell"
            currentInput = "Not Today 😈"
            currentView?.text = currentInput
            justCalculated = true
            val resId = resources.getIdentifier("evil_laugh", "raw", packageName)
            if (resId != 0) {
                activeMediaPlayer?.release()
                activeMediaPlayer = MediaPlayer.create(this, resId)
                activeMediaPlayer?.start()
            }
            shakeScreen()
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }
    }
    return false
}
