package com.example.calculator

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.concurrent.thread

var activeMediaPlayer: MediaPlayer? = null // שומר על הנגן בחיים

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
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
    }
    return false
}
