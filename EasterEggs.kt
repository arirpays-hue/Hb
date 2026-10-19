package com.example.calculator

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.thread

var activeMediaPlayer: MediaPlayer? = null // שומר על הנגן בחיים

fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        "123456789" -> {
            justCalculated = true
            try { 
                val resId = resources.getIdentifier("cheer", "raw", packageName)
                if (resId != 0) {
                    activeMediaPlayer?.release()
                    activeMediaPlayer = MediaPlayer.create(this, resId)
                    activeMediaPlayer?.start()
                }
            } catch (e: Exception) {}
            
            thread {
                val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN)
                for (i in 1..15) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors.random()) }; Thread.sleep(100) }
                Handler(Looper.getMainLooper()).post { applyTheme() }
            }
            return true
        }
        "911" -> {
            justCalculated = true
            try { 
                val resId = resources.getIdentifier("siren", "raw", packageName)
                if (resId != 0) {
                    activeMediaPlayer?.release()
                    activeMediaPlayer = MediaPlayer.create(this, resId)
                    activeMediaPlayer?.start()
                }
            } catch (e: Exception) {}
            
            thread {
                val colors = listOf(Color.RED, Color.BLUE)
                for (i in 1..20) { Handler(Looper.getMainLooper()).post { mainContent?.setBackgroundColor(colors[i % 2]) }; Thread.sleep(150) }
                Handler(Looper.getMainLooper()).post { applyTheme() }
            }
            return true
        }
    }
    return false
}
