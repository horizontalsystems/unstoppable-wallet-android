package io.horizontalsystems.chartview

import android.graphics.*
import androidx.core.graphics.ColorUtils.setAlphaComponent
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.models.ChartConfig

class ChartGradient(private val animator: ChartAnimator) : ChartDraw {

    private var shape = RectF(0f, 0f, 0f, 0f)
    private var points = listOf<Coordinate>()

    private var shadeMaxY = 0f
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    fun setPoints(coordinates: List<Coordinate>) {
        points = coordinates
    }

    fun setShader(color: Int) {
        setGradient(setAlphaComponent(color, 0xCC), setAlphaComponent(color, 0x0D))
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
            if (index == 0) {
                path.moveTo(point.x, animator.getAnimatedY(point.y, shape.bottom))
            } else {
                path.lineTo(point.x, animator.getAnimatedY(point.y, shape.bottom))
            }
        }

        //  Link the last two points
        path.lineTo(points.last().x, shape.bottom)
        path.lineTo(points.first().x, shape.bottom)
        path.close()

        drawPath(path, paint)
    }

    private fun setGradient(colorStart: Int, colorEnd: Int) {
        paint.shader = LinearGradient(0f, 0f, 0f, shape.bottom + 2, colorStart, colorEnd, Shader.TileMode.REPEAT)
    }
}
