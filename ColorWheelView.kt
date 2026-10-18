package com.example.calculator

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

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
