package io.horizontalsystems.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartGradient(private val animator: ChartAnimator? = null, override var isVisible: Boolean = true) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var points = listOf<PointF>()

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun setPoints(list: List<PointF>) {
        points = list
    }

    fun setShader(gradient: ChartConfig.GradientColor) {
        setGradient(setAlphaComponent(gradient.startColor, 0x0D), setAlphaComponent(gradient.endColor, 0x80))
    }

    fun setShape(rect: RectF) {
        shape = rect
    }

    override fun draw(canvas: Canvas) {
        canvas.drawGradient()
    }

    private fun Canvas.drawGradient() {
        if (points.size < 2) return
        val path = Path()

        points.forEachIndexed { index, point ->
            when (index) {
                0 -> path.moveTo(point.x, getY(point))
                else -> path.lineTo(point.x, getY(point))
            }
        }

        //  Link the last two points
        path.lineTo(points.last().x, shape.bottom)
        path.lineTo(points.first().x, shape.bottom)
        path.close()

        drawPath(path, paint)
    }

    private fun getY(point: PointF) : Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        paint.shader = LinearGradient(0f, 0f, shape.width(), 0f, colorStart, colorEnd, Shader.TileMode.REPEAT)
    }
}
