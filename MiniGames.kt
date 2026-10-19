package com.example.calculator

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.random.Random

object MiniGameEngine {
    var isActive = false
    private var currentGame = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameLoop: Runnable? = null

    // משתנים לאיקס עיגול (1337)
    private val tttBoard = Array(9) { "" }

    // משתנים לפולשי החשבון (8080)
    private var invaderScore = 0
    private var invaderTrack = mutableListOf<String>()
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    // משתנים למירוץ מכוניות (5555)
    private var carLane = 1 // 0 = שמאל, 1 = אמצע, 2 = ימין
    private var racerScore = 0
    private var obstacleLane = 1
    private var obstacleType = "×" // × = מכשול, + = בונוס
    private var obstacleDist = 5

    // משתנים לנטרול פצצה (1010)
    private var bombTimeLeft = 15
    private var bombScore = 0
    private var expectedOp = "×"

    fun stopGame(activity: MainActivity, message: String = "יצאת מהמשחק 🎮") {
        isActive = false
        currentGame = ""
        gameLoop?.let { handler.removeCallbacks(it) }
        activity.currentInput = ""
        activity.justCalculated = false
        activity.applyTheme() // משחזר את כפתורי המקלדת והתצוגה
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    // --- משחק 1: איקס עיגול על מקלדת המחשבון (קוד 1337) ---
    fun startTicTacToe(activity: MainActivity) {
        stopPreviousLoop()
        isActive = true
        currentGame = "TTT"
        for (i in 0..8) tttBoard[i] = (i + 1).toString()
        updateTicTacToeUI(activity, "תורך! לחץ על 1-9 להצבת × (AC ליציאה)")
    }

    private fun updateTicTacToeUI(activity: MainActivity, status: String) {
        activity.memoryIndicator?.text = "🎮 איקס עיגול (× נגד 0) | AC ליציאה"
        activity.expressionView?.text = status
        val row1 = "[ ${tttBoard[6]} | ${tttBoard[7]} | ${tttBoard[8]} ]"
        val row2 = "[ ${tttBoard[3]} | ${tttBoard[4]} | ${tttBoard[5]} ]"
        val row3 = "[ ${tttBoard[0]} | ${tttBoard[1]} | ${tttBoard[2]} ]"
        activity.currentView?.text = "$row1\n$row2\n$row3"

        // עדכון הכפתורים הפיזיים במקלדת!
        for (i in 0..8) {
            val btn = activity.keypadButtons[(i + 1).toString()]
            val mark = tttBoard[i]
            btn?.text = mark
            when (mark) {
                "×" -> btn?.setTextColor(Color.GREEN)
                "0" -> btn?.setTextColor(Color.RED)
                else -> btn?.setTextColor(Color.WHITE)
            }
        }
    }

    private fun checkWinner(): String? {
        val wins = arrayOf(
            intArrayOf(0,1,2), intArrayOf(3,4,5), intArrayOf(6,7,8),
            intArrayOf(0,3,6), intArrayOf(1,4,7), intArrayOf(2,5,8),
            intArrayOf(0,4,8), intArrayOf(2,4,6)
        )
        for (w in wins) {
            if (tttBoard[w[0]] == tttBoard[w[1]] && tttBoard[w[1]] == tttBoard[w[2]]) {
                return tttBoard[w[0]]
            }
        }
        if (tttBoard.none { it in "1".."9" }) return "TIE"
        return null
    }

    // --- משחק 2: פולשי החשבון במחשבון המדעי (קוד 8080) ---
    fun startCalcInvaders(activity: MainActivity) {
        stopPreviousLoop()
        isActive = true
        currentGame = "INVADERS"
        invaderScore = 0
        invaderTrack = MutableList(6) { "." }
        activity.memoryIndicator?.text = "👾 פולשי החשבון | לחץ על הסימן כדי ליירט! | AC ליציאה"

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "INVADERS") return
                // קידום הפולשים לכיוון החללית
                val reachedShip = invaderTrack.removeAt(0)
                if (reachedShip != ".") {
                    activity.shakeScreen()
                    stopGame(activity, "💥 החללית נפגעה! ניקוד סופי: $invaderScore")
                    return
                }
                val nextSpawn = if (Random.nextFloat() < 0.65f) invaderSymbols.random() else "."
                invaderTrack.add(nextSpawn)
                renderInvaders(activity)
                val speed = (950L - (invaderScore * 25L)).coerceAtLeast(350L)
                handler.postDelayed(this, speed)
            }
        }
        renderInvaders(activity)
        handler.postDelayed(gameLoop!!, 900)
    }

    private fun renderInvaders(activity: MainActivity) {
        activity.expressionView?.text = "ניקוד: $invaderScore ⭐ | יירט את הסימן הקרוב ל-🚀"
        activity.currentView?.text = "🚀 ⬅ " + invaderTrack.joinToString("  ")
    }

    // --- משחק 3: מירוץ מכוניות מתמטי (קוד 5555) ---
    fun startMathRacer(activity: MainActivity) {
        stopPreviousLoop()
        isActive = true
        currentGame = "RACER"
        carLane = 1
        racerScore = 0
        obstacleLane = Random.nextInt(3)
        obstacleDist = 4
        obstacleType = if (Random.nextBoolean()) "×" else "+"
        activity.memoryIndicator?.text = "🏎️ מירוץ: 4=שמאל, 6=ימין, ×=פוצץ מכשול, +=טורבו"

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "RACER") return
                obstacleDist--
                if (obstacleDist <= 0) {
                    if (obstacleLane == carLane) {
                        if (obstacleType == "×") {
                            activity.shakeScreen()
                            stopGame(activity, "💥 התנגשת במחסום ×! ניקוד: $racerScore")
                            return
                        } else {
                            racerScore += 5
                        }
                    } else {
                        racerScore++
                    }
                    obstacleDist = 4
                    obstacleLane = Random.nextInt(3)
                    obstacleType = if (Random.nextFloat() < 0.7f) "×" else "+"
                }
                renderRacer(activity)
                val speed = (750L - (racerScore * 15L)).coerceAtLeast(280L)
                handler.postDelayed(this, speed)
            }
        }
        renderRacer(activity)
        handler.postDelayed(gameLoop!!, 750)
    }

    private fun renderRacer(activity: MainActivity) {
        val lanes = arrayOf(" |   | ", " |   | ", " |   | ")
        lanes[carLane] = " |🏎️| "
        val obsLanes = arrayOf("  .  ", "  .  ", "  .  ")
        obsLanes[obstacleLane] = " [$obstacleType] "
        activity.expressionView?.text = "מרחק: ${obstacleDist}מ' | ${obsLanes.joinToString("")} | ניקוד: $racerScore"
        activity.currentView?.text = lanes.joinToString("")
    }

    // --- משחק 4: נטרול פצצה מתמטית (קוד 1010) ---
    fun startBombDefusal(activity: MainActivity) {
        stopPreviousLoop()
        isActive = true
        currentGame = "BOMB"
        bombTimeLeft = 15
        bombScore = 0
        activity.memoryIndicator?.text = "💣 נטרל את הפצצה! לחץ על +, −, × או ÷ | AC ליציאה"
        generateBombQuestion(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "BOMB") return
                bombTimeLeft--
                if (bombTimeLeft <= 0) {
                    activity.shakeScreen()
                    stopGame(activity, "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore")
                    return
                }
                activity.expressionView?.text = "⏳ זמן: ${bombTimeLeft}ש' | נוטרלו: $bombScore 💣"
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(gameLoop!!, 1000)
    }

    private fun generateBombQuestion(activity: MainActivity) {
        val a = Random.nextInt(2, 12)
        val b = Random.nextInt(2, 10)
        val ops = listOf("+", "−", "×", "÷")
        expectedOp = ops.random()
        val displayEq = when (expectedOp) {
            "+" -> "$a [?] $b = ${a + b}"
            "−" -> "${a + b} [?] $b = $a"
            "×" -> "$a [?] $b = ${a * b}"
            else -> "${a * b} [?] $b = $a"
        }
        activity.expressionView?.text = "⏳ זמן: ${bombTimeLeft}ש' | נוטרלו: $bombScore 💣"
        activity.currentView?.text = displayEq
    }

    // --- קליטת לחיצות מהמקלדת בזמן משחק ---
    fun onKeyPress(activity: MainActivity, key: String): Boolean {
        if (!isActive) return false
        if (key == "AC") {
            stopGame(activity)
            return true
        }

        when (currentGame) {
            "TTT" -> {
                val idx = key.toIntOrNull()?.minus(1) ?: return true
                if (idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
                    tttBoard[idx] = "×"
                    var w = checkWinner()
                    if (w != null) {
                        finishTicTacToe(activity, w)
                        return true
                    }
                    // תור המחשב (מציב 0)
                    val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
                    if (free.isNotEmpty()) {
                        tttBoard[free.random()] = "0"
                    }
                    w = checkWinner()
                    if (w != null) {
                        finishTicTacToe(activity, w)
                    } else {
                        updateTicTacToeUI(activity, "תורך! בחר משבצת פנויה (1-9)")
                    }
                }
                return true
            }

            "INVADERS" -> {
                val targetIndex = invaderTrack.indexOfFirst { it != "." }
                if (targetIndex != -1 && invaderTrack[targetIndex] == key) {
                    invaderTrack[targetIndex] = "."
                    invaderScore += 10
                    if (activity.useHaptic) activity.mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    renderInvaders(activity)
                } else {
                    invaderScore = (invaderScore - 2).coerceAtLeast(0)
                    renderInvaders(activity)
                }
                return true
            }

            "RACER" -> {
                when (key) {
                    "4" -> carLane = (carLane - 1).coerceAtLeast(0)
                    "6" -> carLane = (carLane + 1).coerceAtMost(2)
                    "×" -> if (obstacleLane == carLane && obstacleType == "×") {
                        obstacleType = "+" // פיצוץ המחסום והפיכתו לבונוס!
                        racerScore += 2
                    }
                    "+" -> racerScore++
                }
                renderRacer(activity)
                return true
            }

            "BOMB" -> {
                if (key in listOf("+", "−", "×", "÷")) {
                    if (key == expectedOp) {
                        bombScore++
                        bombTimeLeft = (bombTimeLeft + 3).coerceAtMost(20)
                        generateBombQuestion(activity)
                    } else {
                        bombTimeLeft = (bombTimeLeft - 3).coerceAtLeast(1)
                        activity.shakeScreen()
                    }
                }
                return true
            }
        }
        return true
    }

    private fun finishTicTacToe(activity: MainActivity, winner: String) {
        val msg = when (winner) {
            "×" -> "🏆 ניצחת! × ניצח את 0! (לחץ על מקש כלשהו להתחלה מחדש או AC ליציאה)"
            "0" -> "🤖 המחשב (0) ניצח! נסה שוב"
            else -> "🤝 תיקו! משחק צמוד"
        }
        updateTicTacToeUI(activity, msg)
        handler.postDelayed({ if (isActive && currentGame == "TTT") startTicTacToe(activity) }, 2200)
    }

    private fun stopPreviousLoop() {
        gameLoop?.let { handler.removeCallbacks(it) }
    }
}
