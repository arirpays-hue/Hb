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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#0D0F14")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D0F14")

        setContent {
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
        if (value.isNaN() || value.isInfinite()) return "שגיאה"
        val df = DecimalFormat("#.########")
        return df.format(value)
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
        val expression = "$previousInput $op $currentInput"
        if (formatted != "שגיאה") {
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
                if (justCalculated || currentInput == "שגיאה") {
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
                if (currentInput == "שגיאה") return
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
                if (justCalculated || currentInput == "שגיאה") {
                    currentInput = "0."
                    justCalculated = false
                } else if (!currentInput.contains(".")) {
                    currentInput += "."
                }
            }
            else -> { // Digits 0-9
                if (justCalculated || currentInput == "0" || currentInput == "שגיאה") {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // כרטיס תצוגה עליון עם אנימציות
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
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                        text = if (showHistory) "סגור היסטוריה ✕" else "היסטוריה (${history.size}) 🕒",
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

            // מגירת היסטוריה מונפשת
            AnimatedVisibility(
                visible = showHistory,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("אין חישובים קודמים עדיין", color = Color(0xFF6C7589), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                        showHistory = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "= ${item.second}", color = Color(0xFF00E676), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text(text = item.first, color = Color(0xFF9EA7B8), fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            // אזור המספרים והתוצאה
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                val expressionText = if (operator != null) "$previousInput $operator" else ""
                AnimatedContent(
                    targetState = expressionText,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically { -it / 2 })
                            .togetherWith(fadeOut(tween(120)) + slideOutVertically { it / 2 })
                    },
                    label = "ExpressionAnimation"
                ) { targetExpr ->
                    Text(
                        text = targetExpr,
                        color = Color(0xFF8A94A8),
                        fontSize = 24.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedContent(
                    targetState = currentInput,
                    transitionSpec = {
                        (slideInVertically(animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)) { height -> height / 3 } + fadeIn(tween(150)))
                            .togetherWith(slideOutVertically(tween(120)) { height -> -height / 3 } + fadeOut(tween(100)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "ResultAnimation"
                ) { targetNumber ->
                    val dynamicFontSize = when {
                        targetNumber.length > 10 -> 40.sp
                        targetNumber.length > 7 -> 48.sp
                        else -> 60.sp
                    }
                    Text(
                        text = targetNumber,
                        color = Color.White,
                        fontSize = dynamicFontSize,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // לוח המקשים עם אנימציות קפיץ ושינוי צורה
        val rows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { symbol ->
                        val weight = if (symbol == "0") 2f else 1f
                        val isSelectedOperator = (operator == symbol && justCalculated)
                        CalculatorButton(
                            symbol = symbol,
                            isSelected = isSelectedOperator,
                            modifier = Modifier
                                .weight(weight)
                                .height(78.dp),
                            onClick = { onButtonClick(symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // אנימציית קפיץ לגודל הכפתור בעת לחיצה
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonScale"
    )

    // אנימציית שינוי צורה מעיגול לריבוע מעוגל (Morphing)
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed || isSelected) 18.dp else 40.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ButtonCorner"
    )

    val defaultBg = when (symbol) {
        "AC" -> Color(0xFF422026)
        "⌫", "%" -> Color(0xFF283042)
        "÷", "×", "−", "+" -> if (isSelected) Color(0xFF64B5F6) else Color(0xFF1D3557)
        "=" -> Color(0xFF00E676)
        else -> Color(0xFF1B1F2B)
    }

    val pressedBg = when (symbol) {
        "AC" -> Color(0xFF642B35)
        "=" -> Color(0xFF69F0AE)
        "÷", "×", "−", "+" -> Color(0xFF42A5F5)
        else -> Color(0xFF2E3548)
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) pressedBg else defaultBg,
        animationSpec = tween(durationMillis = 120),
        label = "ButtonColor"
    )

    val textColor = when (symbol) {
        "AC" -> Color(0xFFFF6B6B)
        "⌫", "%" -> Color(0xFF00E676)
        "÷", "×", "−", "+" -> if (isSelected) Color(0xFF0D0F14) else Color(0xFF82B1FF)
        "=" -> Color(0xFF0D0F14)
        else -> Color.White
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        modifier = modifier.scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = symbol,
                color = textColor,
                fontSize = if (symbol in listOf("÷", "×", "−", "+", "=")) 32.sp else 26.sp,
                fontWeight = if (symbol == "=") FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

