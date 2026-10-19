package com.example.calculator

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        "123456789" -> {
            justCalculated = true
            try { val resId = resources.getIdentifier("cheer", "raw", packageName); if (resId != 0) MediaPlayer.create(this, resId)?.start() } catch (e: Exception) {}
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
            // מנגן את סירנת המשטרה
            try { val resId = resources.getIdentifier("siren", "raw", packageName); if (resId != 0) MediaPlayer.create(this, resId)?.start() } catch (e: Exception) {}
            val orig = bgColor
            thread {
                // מהבהב אדום וכחול כמו ניידת
                val colors = listOf(Color.RED, Color.BLUE)
                for (i in 1..20) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(150) }
                Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(orig) }
            }
            return true
        }
    }
    return false
}
