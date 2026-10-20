package com.example.calculator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import kotlin.math.sin

object MiniGameEngine {
    var isActive = false
    var currentGame = ""
    val handler = Handler(Looper.getMainLooper())
    var gameLoop: Runnable? = null
    var savedTextSizeSp: Float = 36f

    fun stopGame(activity: MainActivity, message: String = "יצאת מהמשחק 🎮") {
        isActive = false
        currentGame = ""
        GameLogicAction.isAnimalMode = false
        gameLoop?.let { handler.removeCallbacks(it) }
        GameLogicAction.restoreButtonLabels(activity)
        activity.currentView?.textSize = savedTextSizeSp
        activity.currentInput = ""
        activity.justCalculated = false
        activity.applyTheme()
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    fun saveDisplayState(activity: MainActivity, customSizeSp: Float? = null) {
        val dm = activity.resources.displayMetrics
        val currentPx = activity.currentView?.textSize ?: (36f * dm.scaledDensity)
        savedTextSizeSp = currentPx / dm.scaledDensity
        customSizeSp?.let {
            activity.currentView?.setSingleLine(false)
            activity.currentView?.maxLines = 8
            activity.currentView?.textSize = it
        }
    }

    fun stopPreviousLoop() {
        gameLoop?.let { handler.removeCallbacks(it) }
    }

    fun isScientificModeActive(activity: MainActivity): Boolean {
        val sinBtn = activity.keypadButtons["sin"]
        return sinBtn != null && sinBtn.visibility == View.VISIBLE && sinBtn.isShown
    }

    fun playSqueakSound() {
        Thread {
            try {
                val sampleRate = 22050
                val durationMs = 140
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = if (progress < 0.45) {
                        1100.0 + (progress / 0.45) * 1600.0
                    } else {
                        2700.0 - ((progress - 0.45) / 0.55) * 1300.0
                    }
                    val envelope = sin(progress * Math.PI)
                    phase += 2.0 * Math.PI * freq / sampleRate
                    val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * envelope
                    buffer[i] = (sample * Short.MAX_VALUE * 0.7).toInt().toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 30)
                track.release()
            } catch (_: Exception) {}
        }.start()
    }

    // הנתב הראשי של כל הלחיצות במשחקים
    fun onKeyPress(activity: MainActivity, key: String): Boolean {
        if (!isActive) return false
        if (key == "AC") {
            stopGame(activity)
            return true
        }

        when (currentGame) {
            "ANIMALS" -> return GameLogicAction.onAnimalKeyPress(activity)
            "TTT" -> return GameLogicTTTAndBomb.onTTTKeyPress(activity, key)
            "INVADERS" -> return GameLogicAction.onInvadersKeyPress(activity, key)
            "RACER" -> return GameLogicAction.onRacerKeyPress(activity, key)
            "BOMB" -> return GameLogicTTTAndBomb.onBombKeyPress(activity, key)
        }
        return true
    }
}
package com.example.calculator

import android.graphics.Color
import kotlin.random.Random

object GameLogicTTTAndBomb {

    // --- איקס עיגול ---
    private val tttBoard = Array(9) { "" }

    fun startTicTacToe(activity: MainActivity) {
        MiniGameEngine.stopPreviousLoop()
        MiniGameEngine.isActive = true
        MiniGameEngine.currentGame = "TTT"
        MiniGameEngine.saveDisplayState(activity, 24f)
        for (i in 0..8) tttBoard[i] = (i + 1).toString()
        updateTicTacToeUI(activity, "תורך! לחץ 1-9 להצבת × (AC ליציאה)")
    }

    private fun updateTicTacToeUI(activity: MainActivity, status: String) {
        activity.memoryIndicator?.text = "🎮 איקס עיגול | AC ליציאה"
        activity.expressionView?.text = status
        val row1 = "[ ${tttBoard[6]} │ ${tttBoard[7]} │ ${tttBoard[8]} ]"
        val row2 = "[ ${tttBoard[3]} │ ${tttBoard[4]} │ ${tttBoard[5]} ]"
        val row3 = "[ ${tttBoard[0]} │ ${tttBoard[1]} │ ${tttBoard[2]} ]"
        activity.currentView?.text = "$row1\n$row2\n$row3"

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

    private fun checkWinnerOnBoard(board: Array<String>): String? {
        val wins = arrayOf(
            intArrayOf(0,1,2), intArrayOf(3,4,5), intArrayOf(6,7,8),
            intArrayOf(0,3,6), intArrayOf(1,4,7), intArrayOf(2,5,8),
            intArrayOf(0,4,8), intArrayOf(2,4,6)
        )
        for (w in wins) {
            if (board[w[0]] == board[w[1]] && board[w[1]] == board[w[2]]) {
                return board[w[0]]
            }
        }
        if (board.none { it in "1".."9" }) return "TIE"
        return null
    }

    private fun getSmartComputerMove(): Int {
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (free.isEmpty()) return -1

        for (i in free) {
            val orig = tttBoard[i]
            tttBoard[i] = "0"
            if (checkWinnerOnBoard(tttBoard) == "0") { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        for (i in free) {
            val orig = tttBoard[i]
            tttBoard[i] = "×"
            if (checkWinnerOnBoard(tttBoard) == "×") { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }

        if (Random.nextFloat() < 0.82f) {
            var bestScore = Int.MIN_VALUE
            var bestMoves = mutableListOf<Int>()
            for (i in free) {
                val orig = tttBoard[i]
                tttBoard[i] = "0"
                val score = minimax(tttBoard, 0, false)
                tttBoard[i] = orig
                if (score > bestScore) {
                    bestScore = score
                    bestMoves.clear()
                    bestMoves.add(i)
                } else if (score == bestScore) {
                    bestMoves.add(i)
                }
            }
            if (bestMoves.isNotEmpty()) return bestMoves.random()
        }
        return free.random()
    }

    private fun minimax(board: Array<String>, depth: Int, isMaximizing: Boolean): Int {
        when (checkWinnerOnBoard(board)) {
            "0" -> return 10 - depth
            "×" -> return depth - 10
            "TIE" -> return 0
        }
        val free = (0..8).filter { board[it] !in listOf("×", "0") }
        if (isMaximizing) {
            var best = Int.MIN_VALUE
            for (i in free) {
                val orig = board[i]
                board[i] = "0"
                best = maxOf(best, minimax(board, depth + 1, false))
                board[i] = orig
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (i in free) {
                val orig = board[i]
                board[i] = "×"
                best = minOf(best, minimax(board, depth + 1, true))
                board[i] = orig
            }
            return best
        }
    }

    fun onTTTKeyPress(activity: MainActivity, key: String): Boolean {
        val idx = key.toIntOrNull()?.minus(1) ?: return true
        if (idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
            tttBoard[idx] = "×"
            var w = checkWinnerOnBoard(tttBoard)
            if (w != null) {
                finishTicTacToe(activity, w)
                return true
            }
            val compMove = getSmartComputerMove()
            if (compMove != -1) {
                tttBoard[compMove] = "0"
            }
            w = checkWinnerOnBoard(tttBoard)
            if (w != null) {
                finishTicTacToe(activity, w)
            } else {
                updateTicTacToeUI(activity, "תורך! בחר משבצת פנויה (1-9)")
            }
        }
        return true
    }

    private fun finishTicTacToe(activity: MainActivity, winner: String) {
        val msg = when (winner) {
            "×" -> "🏆 ניצחת את המחשב! (AC ליציאה)"
            "0" -> "🤖 המחשב ניצח הפעם! נסה שוב"
            else -> "🤝 תיקו!"
        }
        updateTicTacToeUI(activity, msg)
        MiniGameEngine.handler.postDelayed({ if (MiniGameEngine.isActive && MiniGameEngine.currentGame == "TTT") startTicTacToe(activity) }, 2500)
    }

    // --- נטרול פצצה ---
    private var bombTimeLeft = 15
    private var bombScore = 0
    private var expectedOp = "×"

    fun startBombDefusal(activity: MainActivity) {
        MiniGameEngine.stopPreviousLoop()
        MiniGameEngine.isActive = true
        MiniGameEngine.currentGame = "BOMB"
        MiniGameEngine.saveDisplayState(activity, 26f)
        bombTimeLeft = 15
        bombScore = 0
        activity.memoryIndicator?.text = "💣 נטרל את הפצצה! לחץ על +, −, × או ÷ | AC ליציאה"
        generateBombQuestion(activity)

        MiniGameEngine.gameLoop = object : Runnable {
            override fun run() {
                if (!MiniGameEngine.isActive || MiniGameEngine.currentGame != "BOMB") return
                bombTimeLeft--
                if (bombTimeLeft <= 0) {
                    activity.shakeScreen()
                    MiniGameEngine.stopGame(activity, "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore")
                    return
                }
                activity.expressionView?.text = "⏳ זמן: ${bombTimeLeft}ש' | נוטרלו: $bombScore 💣"
                MiniGameEngine.handler.postDelayed(this, 1000)
            }
        }
        MiniGameEngine.handler.postDelayed(MiniGameEngine.gameLoop!!, 1000)
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
        activity.currentView?.text = "\u202D$displayEq\u202C"
    }

    fun onBombKeyPress(activity: MainActivity, key: String): Boolean {
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
package com.example.calculator

import android.view.HapticFeedbackConstants
import android.widget.Toast
import kotlin.random.Random

object GameLogicAction {

    // --- ביצת פסחא חיות ---
    var isAnimalMode = false
    private val savedButtonTexts = mutableMapOf<String, CharSequence>()
    val animalEmojis = listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸")

    fun startAnimalParty(activity: MainActivity) {
        MiniGameEngine.stopPreviousLoop()
        MiniGameEngine.isActive = true
        MiniGameEngine.currentGame = "ANIMALS"
        isAnimalMode = true
        MiniGameEngine.saveDisplayState(activity)

        savedButtonTexts.clear()
        val shuffledAnimals = animalEmojis.shuffled()
        var idx = 0
        for ((key, btn) in activity.keypadButtons) {
            if (btn != null) {
                savedButtonTexts[key] = btn.text
                if (key != "AC") {
                    btn.text = shuffledAnimals[idx % shuffledAnimals.size]
                    idx++
                }
            }
        }

        activity.memoryIndicator?.text = "🐾 מסיבת חיות! לחץ על המקשים | AC ליציאה"
        activity.currentView?.text = "🐶 🐱 🐸 🦊 🐼"
        MiniGameEngine.playSqueakSound()

        var secondsLeft = 15
        MiniGameEngine.gameLoop = object : Runnable {
            override fun run() {
                if (!MiniGameEngine.isActive || MiniGameEngine.currentGame != "ANIMALS") return
                secondsLeft--
                if (secondsLeft <= 0) {
                    MiniGameEngine.stopGame(activity, "🐾 החיות חזרו לגן החיות!")
                    return
                }
                activity.expressionView?.text = "חוזר למצב רגיל בעוד $secondsLeft שניות..."
                MiniGameEngine.handler.postDelayed(this, 1000)
            }
        }
        MiniGameEngine.handler.postDelayed(MiniGameEngine.gameLoop!!, 1000)
    }

    fun restoreButtonLabels(activity: MainActivity) {
        for ((key, text) in savedButtonTexts) {
            activity.keypadButtons[key]?.text = text
        }
        savedButtonTexts.clear()
    }

    fun onAnimalKeyPress(activity: MainActivity): Boolean {
        MiniGameEngine.playSqueakSound()
        activity.currentView?.text = List(5) { animalEmojis.random() }.joinToString(" ")
        return true
    }

    // --- פולשי החשבון ---
    private var invaderScore = 0
    private var invaderTrack = mutableListOf<String>()
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    fun startCalcInvaders(activity: MainActivity) {
        if (!MiniGameEngine.isScientificModeActive(activity)) {
            Toast.makeText(activity, "🔬 זמין במחשבון המדעי בלבד!", Toast.LENGTH_LONG).show()
            activity.currentInput = ""
            activity.updateDisplay()
            return
        }
        MiniGameEngine.stopPreviousLoop()
        MiniGameEngine.isActive = true
        MiniGameEngine.currentGame = "INVADERS"
        MiniGameEngine.saveDisplayState(activity, 24f)
        invaderScore = 0
        invaderTrack = MutableList(6) { "." }
        activity.memoryIndicator?.text = "👾 פולשי החשבון | יירוט סימנים"

        MiniGameEngine.gameLoop = object : Runnable {
            override fun run() {
                if (!MiniGameEngine.isActive || MiniGameEngine.currentGame != "INVADERS") return
                val reachedShip = invaderTrack.removeAt(0)
                if (reachedShip != ".") {
                    activity.shakeScreen()
                    MiniGameEngine.stopGame(activity, "💥 החללית נפגעה! ניקוד: $invaderScore")
                    return
                }
                val nextSpawn = if (Random.nextFloat() < 0.65f) invaderSymbols.random() else "."
                invaderTrack.add(nextSpawn)
                renderInvaders(activity)
                val speed = (950L - (invaderScore * 25L)).coerceAtLeast(350L)
                MiniGameEngine.handler.postDelayed(this, speed)
            }
        }
        renderInvaders(activity)
        MiniGameEngine.handler.postDelayed(MiniGameEngine.gameLoop!!, 900)
    }

    private fun renderInvaders(activity: MainActivity) {
        activity.expressionView?.text = "ניקוד: $invaderScore ⭐"
        activity.currentView?.text = "\u202D🚀 ⬅ " + invaderTrack.joinToString("  ") + "\u202C"
    }

    fun onInvadersKeyPress(activity: MainActivity, key: String): Boolean {
        val targetIndex = invaderTrack.indexOfFirst { it != "." }
        if (targetIndex != -1 && invaderTrack[targetIndex] == key) {
            invaderTrack[targetIndex] = "."
            invaderScore += 10
            if (activity.useHaptic) activity.mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } else {
            invaderScore = (invaderScore - 2).coerceAtLeast(0)
        }
        renderInvaders(activity)
        return true
    }

    // --- מירוץ מכוניות ---
    private var carLane = 1
    private var racerScore = 0
    private val roadRows = MutableList(5) { Pair(-1, "") }

    fun startMathRacer(activity: MainActivity) {
        MiniGameEngine.stopPreviousLoop()
        MiniGameEngine.isActive = true
        MiniGameEngine.currentGame = "RACER"
        MiniGameEngine.saveDisplayState(activity, 18f)
        carLane = 1
        racerScore = 0
        for (i in 0 until 5) roadRows[i] = Pair(-1, "")
        spawnRoadObstacle()

        activity.memoryIndicator?.text = "🏎️ 4=⬅️ | 6=➡️ | 5=💥 | 8=⚡ | AC=יציאה"

        MiniGameEngine.gameLoop = object : Runnable {
            override fun run() {
                if (!MiniGameEngine.isActive || MiniGameEngine.currentGame != "RACER") return
                advanceRoad(activity)
                if (!MiniGameEngine.isActive) return
                renderRacer(activity)
                val speed = (700L - (racerScore * 10L)).coerceAtLeast(280L)
                MiniGameEngine.handler.postDelayed(this, speed)
            }
        }
        renderRacer(activity)
        MiniGameEngine.handler.postDelayed(MiniGameEngine.gameLoop!!, 700)
    }

    private fun spawnRoadObstacle() {
        if (roadRows[1].first == -1 && Random.nextFloat() < 0.65f) {
            val lane = Random.nextInt(3)
            val type = if (Random.nextFloat() < 0.72f) "×" else "+"
            roadRows[0] = Pair(lane, type)
        } else {
            roadRows[0] = Pair(-1, "")
        }
    }

    private fun advanceRoad(activity: MainActivity) {
        for (i in 4 downTo 1) {
            roadRows[i] = roadRows[i - 1]
        }
        spawnRoadObstacle()
        val bottomEvent = roadRows[4]
        if (bottomEvent.first != -1) {
            if (bottomEvent.first == carLane) {
                if (bottomEvent.second == "×") {
                    activity.shakeScreen()
                    MiniGameEngine.stopGame(activity, "💥 התנגשת! ניקוד סופי: $racerScore 🏆")
                    return
                } else if (bottomEvent.second == "+") {
                    racerScore += 5
                    roadRows[4] = Pair(-1, "")
                }
            } else {
                racerScore += 1
            }
        }
    }

    private fun renderRacer(activity: MainActivity) {
        activity.expressionView?.text = "🏆 ניקוד: $racerScore | 4=⬅️ 5=💥 8=⚡ 6=➡️"
        val sb = StringBuilder()
        for (r in 0 until 5) {
            val (obsLane, obsType) = roadRows[r]
            val cells = Array(3) { lane ->
                when {
                    r == 4 && lane == carLane -> "🏎️"
                    obsLane == lane -> if (obsType == "×") "🚧" else "⭐"
                    else -> " · "
                }
            }
            sb.append("\u202D║ ").append(cells[0]).append(" │ ").append(cells[1]).append(" │ ").append(cells[2]).append(" ║\u202C")
            if (r < 4) sb.append("\n")
        }
        activity.currentView?.text = sb.toString()
    }

    fun onRacerKeyPress(activity: MainActivity, key: String): Boolean {
        when (key) {
            "4" -> carLane = (carLane - 1).coerceAtLeast(0)
            "6" -> carLane = (carLane + 1).coerceAtMost(2)
            "5", "×" -> {
                for (r in 3 downTo 0) {
                    if (roadRows[r].first == carLane && roadRows[r].second == "×") {
                        roadRows[r] = Pair(carLane, "+")
                        racerScore += 2
                        break
                    }
                }
            }
            "8", "+" -> {
                racerScore++
                advanceRoad(activity)
            }
        }
        if (MiniGameEngine.isActive) renderRacer(activity)
        return true
    }
}
