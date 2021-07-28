package io.horizontalsystems.bankwallet.modules.coin.majorholders

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import io.horizontalsystems.bankwallet.R

class SemiCircleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private var strokeWidth = 0f
    private var proportions = listOf<Triple<Float, Float, Paint>>()

    private val paintColor = context.getColor(R.color.yellow_d)
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var rectangle: RectF? = null

    fun setProportion(vararg props: Float) {
        var startAngle = 180F
        val colorParts = 255 / props.size

        proportions = props.mapIndexed { index, item ->
            val sweepAngle = item / 100 * 180F
            val colorAlpha = 255 - index * colorParts

            val proportion = Triple(startAngle, sweepAngle, Paint(paint).apply {
                color = ColorUtils.setAlphaComponent(paintColor, colorAlpha)
            })

            startAngle += sweepAngle
            proportion
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, width / 2)
    }

    override fun onDraw(canvas: Canvas) {
        if (rectangle == null) {
            strokeWidth = (width / 2) * 0.4f
            val strokeOffset = strokeWidth / 2f
            rectangle = RectF(strokeOffset, strokeOffset, width - strokeOffset, height * 2f - strokeOffset)
        }

        proportions.forEach { (startAngle, sweepAngle, paint) ->
            canvas.drawArc(rectangle!!, startAngle, sweepAngle, false, paint.also {
                it.strokeWidth = strokeWidth
            })
        }

        super.onDraw(canvas)
    }
}
