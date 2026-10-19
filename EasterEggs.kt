package com.example.calculator

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.concurrent.thread

var activeMediaPlayer: MediaPlayer? = null // שומר על הנגן בחיים

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        // --- 🎮 מיני-משחקים אינטראקטיביים בתוך המחשבון ---
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

        // --- הביצים הקיימות (סאונד והבהובים) ---
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
                Toast.makeText(this, "⚠️ הסירנה חסרה! ודא שלא קוראים לקובץ siren.mp3.mp3 בגיטהאב", Toast.LENGTH_LONG).show()
            }
            val orig = bgColor
            thread {
                val colors = listOf(Color.RED, Color.BLUE)
                for (i in 1..20) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(150) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }

        // 🎗️ מחווה ל-7 באוקטובר
        "0710", "710" -> {
            primaryColor = Color.parseColor("#FFD700")
            savePreferences()
            applyTheme()
            expressionView?.text = "07/10/2023"
            currentInput = "🎗️ לא נשכח ולא נסלח 🎗️"
            currentView?.text = currentInput
            justCalculated = true
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

        // 😈 המספר של השטן
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
            } else {
                Toast.makeText(this, "⚠️ קובץ evil_laugh.mp3 חסר בתיקיית raw!", Toast.LENGTH_SHORT).show()
            }
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }
    }
    return false
}
