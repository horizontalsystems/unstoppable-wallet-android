package io.horizontalsystems.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPointF

class ChartGradient(private val animator: ChartAnimator? = null, override var isVisible: Boolean = true) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private var zzz: Zzz? = null

    fun setZzz(zzz: Zzz) {
        this.zzz = zzz
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
        val points = zzz?.getFramePoints() ?: return
        if (points.size < 2) return
        val path = Path()

        points.forEachIndexed { index, point ->
            when (index) {
                0 -> path.moveTo(point.x, point.y)
                else -> path.lineTo(point.x, point.y)
            }
        }

        //  Link the last two points
        path.lineTo(points.last().x, shape.bottom)
        path.lineTo(points.first().x, shape.bottom)
        path.close()

        drawPath(path, paint)
    }

    private fun getY(point: ChartPointF) : Float {
        return when {
            animator != null -> animator.getAnimatedY(point.y, shape.bottom)
            else -> point.y
        }
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        paint.shader = LinearGradient(0f, 0f, shape.width(), 0f, colorStart, colorEnd, Shader.TileMode.REPEAT)
    }
}
