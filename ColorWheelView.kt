package com.example.calculator

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class ColorWheelView(
    context: Context,
    private var hue: Float,
    private var sat: Float,
    private var value: Float,
    private val onHueSatChanged: (Float, Float) -> Unit
) : View(context) {

    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE
        strokeWidth = 3f * context.resources.displayMetrics.density
        color = Color.WHITE 
    }
    private val selFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var cx = 0f; private var cy = 0f; private var r = 0f

    fun updateHSV(h: Float, s: Float, v: Float) { hue = h; sat = s; value = v; invalidate() }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        cx = w / 2f; cy = h / 2f
        r = (Math.min(w, h) / 2f) - (8f * context.resources.displayMetrics.density)
        wheelPaint.shader = ComposeShader(
            SweepGradient(cx, cy, intArrayOf(Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED), null),
            RadialGradient(cx, cy, r, Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP),
            PorterDuff.Mode.SRC_OVER
        )
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawCircle(cx, cy, r, wheelPaint)
        dimPaint.color = Color.argb(((1f - value) * 255).toInt().coerceIn(0, 255), 0, 0, 0)
        c.drawCircle(cx, cy, r, dimPaint)

        val rad = Math.toRadians(hue.toDouble())
        val d = sat * r
        val sx = (cx + d * Math.cos(rad)).toFloat()
        val sy = (cy + d * Math.sin(rad)).toFloat()

        val radiusPx = 11f * context.resources.displayMetrics.density
        selFillPaint.color = Color.HSVToColor(floatArrayOf(hue, sat, value))
        c.drawCircle(sx, sy, radiusPx, selFillPaint)
        c.drawCircle(sx, sy, radiusPx, selPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_DOWN || e.actionMasked == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val dx = e.x - cx; val dy = e.y - cy
            sat = (Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / r).coerceIn(0f, 1f)
            var a = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (a < 0f) a += 360f
            hue = a
            onHueSatChanged(hue, sat)
            invalidate()
            return true
        }
        return super.onTouchEvent(e)
    }
}

fun createColorWheelPicker(context: Context, initialColor: Int, onColorChanged: (Int) -> Unit): View {
    fun dpLocal(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, context.resources.displayMetrics).toInt()

    val hsv = FloatArray(3)
    Color.colorToHSV(initialColor, hsv)
    var currentAlpha = Color.alpha(initialColor)

    val card = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dpLocal(16f), dpLocal(16f), dpLocal(16f), dpLocal(16f))
        background = GradientDrawable().apply {
            setColor(Utils.lightenColor(initialColor, 0.06f))
            cornerRadius = dpLocal(24f).toFloat()
        }
    }

    val previewRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpLocal(12f)
        }
    }

    val previewCircle = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(dpLocal(44f), dpLocal(44f)).apply { leftMargin = dpLocal(12f) }
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(initialColor)
            setStroke(dpLocal(2f), Color.WHITE)
        }
    }

    val hexLabel = TextView(context).apply {
        text = String.format("#%08X", initialColor)
        setTextColor(Color.WHITE)
        textSize = 15f
        typeface = Typeface.MONOSPACE
    }

    previewRow.addView(previewCircle)
    previewRow.addView(hexLabel)

    lateinit var wheelView: ColorWheelView
    fun notifyColorUpdate() {
        val finalColor = Color.HSVToColor(currentAlpha, hsv)
        (previewCircle.background as GradientDrawable).setColor(finalColor)
        hexLabel.text = String.format("#%08X", finalColor)
        wheelView.updateHSV(hsv[0], hsv[1], hsv[2])
        onColorChanged(finalColor)
    }

    wheelView = ColorWheelView(context, hsv[0], hsv[1], hsv[2]) { h, s ->
        hsv[0] = h
        hsv[1] = s
        notifyColorUpdate()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(dpLocal(220f), dpLocal(220f)).apply {
            bottomMargin = dpLocal(14f)
        }
    }

    val brightLabel = TextView(context).apply { text = "☀️ בהירות"; setTextColor(Color.WHITE); textSize = 14f }
    val brightSeek = SeekBar(context).apply {
        max = 100
        progress = (hsv[2] * 100).toInt()
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dpLocal(12f)
        }
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                hsv[2] = p.coerceAtLeast(5) / 100f
                if (u) notifyColorUpdate()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    val alphaLabel = TextView(context).apply { text = "💧 שקיפות"; setTextColor(Color.WHITE); textSize = 14f }
    val alphaSeek = SeekBar(context).apply {
        max = 255
        progress = currentAlpha
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                currentAlpha = p.coerceAtLeast(35)
                if (u) notifyColorUpdate()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    card.addView(previewRow)
    card.addView(wheelView)
    card.addView(brightLabel)
    card.addView(brightSeek)
    card.addView(alphaLabel)
    card.addView(alphaSeek)
    return card
}
