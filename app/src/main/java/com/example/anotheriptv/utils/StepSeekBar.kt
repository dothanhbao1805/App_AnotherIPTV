package com.example.anotheriptv.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar

class StepSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.seekBarStyle
) : AppCompatSeekBar(context, attrs, defStyle) {

    private val dotActivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()  // trắng — phần đã qua
    }

    private val dotInactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFBDBDBD.toInt()  // xám nhạt — phần chưa qua
    }

    private val dotRadius = 2.5f  // kích thước chấm tròn

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (max <= 0) return

        // Không vẽ chấm nếu quá nhiều step
        if (max > 50) return

        val trackLeft  = paddingLeft.toFloat()
        val trackRight = (width - paddingRight).toFloat()
        val trackWidth = trackRight - trackLeft
        val centerY    = height / 2f

        for (i in 0..max) {
            val x     = trackLeft + (i.toFloat() / max) * trackWidth
            val paint = if (i < progress) dotActivePaint else dotInactivePaint
            canvas.drawCircle(x, centerY, dotRadius, paint)
        }
    }
}