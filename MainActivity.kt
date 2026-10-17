@file:OptIn(
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#0D0F14")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D0F14")

        setContent {
            // קיבוע כיוון LTR כדי שהמחשבון לא יתהפך במכשירים בעברית
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                MaterialTheme(
                    colorScheme = darkColorScheme(
                        background = Color(0xFF0D0F14),
                        surface = Color(0xFF181B24)
                    )
                ) {
                    CalculatorScreen()
                }
            }
        }
    }
}

@Composable
fun CalculatorScreen() {
    var currentInput by remember { mutableStateOf("0") }
    var previousInput by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf<String?>(null) }
    var justCalculated by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<Pair<String, String>>() }

    val haptic = LocalHapticFeedback.current

    fun formatResult(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        return try {
            BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
        } catch (e: Exception) {
            "Error"
        }
    }

    fun calculate() {
        val prev = previousInput.toDoubleOrNull()
        val curr = currentInput.toDoubleOrNull()
        val op = operator
        if (prev == null || curr == null || op == null) return

        val res = when (op) {
            "+" -> prev + curr
            "−" -> prev - curr
            "×" -> prev * curr
            "÷" -> if (curr == 0.0) Double.NaN else prev / curr
            "%" -> (prev / 100.0) * curr
            else -> return
        }

        val formatted = formatResult(res)
        val expression = "$previousInput $op$currentInput"
        if (formatted != "Error") {
            history.add(0, expression to formatted)
        }
        currentInput = formatted
        previousInput = ""
        operator = null
        justCalculated = true
    }

    fun onButtonClick(symbol: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        when (symbol) {
            "AC" -> {
                currentInput = "0"
                previousInput = ""
                operator = null
                justCalculated = false
            }
            "⌫" -> {
                if (justCalculated || currentInput == "Error") {
                    currentInput = "0"
                    justCalculated = false
                } else {
                    currentInput = if (currentInput.length > 1) {
                        currentInput.dropLast(1)
                    } else {
                        "0"
                    }
                }
            }
            "+", "−", "×", "÷", "%" -> {
                if (currentInput == "Error") return
                if (previousInput.isNotEmpty() && operator != null && !justCalculated) {
                    calculate()
                }
                previousInput = currentInput
                operator = symbol
                justCalculated = true
            }
            "=" -> {
                if (operator != null && previousInput.isNotEmpty()) {
                    calculate()
                }
            }
            "." -> {
                if (justCalculated || currentInput == "Error") {
                    currentInput = "0."
                    justCalculated = false
                } else if (!currentInput.contains(".")) {
                    currentInput += "."
                }
            }
            else -> { // ספרות 0-9
                if (justCalculated || currentInput == "0" || currentInput == "Error") {
                    currentInput = symbol
                    justCalculated = false
                } else if (currentInput.length < 12) {
                    currentInput += symbol
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0F14))
            .padding(16.dp)
    ) {
        // כרטיס תצוגה עליון - תוקן כך שהמספרים תמיד מוצגים בתחתית הכרטיס
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF181C26), Color(0xFF1E2433))
                    )
                )
                .padding(20.dp)
        ) {
            // שורה עליונה - כפתור היסטוריה
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHistory = !showHistory
                    },
                    shape = RoundedCornerShape(50),
                    color = if (showHistory) Color(0xFF00E676).copy(alpha = 0.2f) else Color(0xFF262C3D)
                ) {
                    Text(
                        text = if (showHistory) "✕ סגור היסטוריה" else "🕒 היסטוריה (${history.size})",
                        color = if (showHistory) Color(0xFF00E676) else Color(0xFFB0B8C8),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }

                if (history.isNotEmpty() && showHistory) {
                    Text(
                        text = "נקה הכל",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                history.clear()
                            }
                            .padding(8.dp)
                    )
                }
            }

            // חלל גמיש או רשימת היסטוריה כשפותחים אותה
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showHistory,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("אין חישובים קודמים עדיין", color = Color(0xFF6C7589), fontSize = 15.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(history) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF141821))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            currentInput = item.second
                                            justCalculated = true
                                            showHistory = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = item.first, color =
