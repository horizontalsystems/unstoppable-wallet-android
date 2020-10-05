package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class ChartView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    enum class ChartType {
        TODAY,
        DAILY,
        WEEKLY,
        WEEKLY2,
        MONTHLY,
        MONTHLY3,
        MONTHLY6,
        MONTHLY12,
        MONTHLY24;
    }

    val shape = RectF(0f,0f,0f,0f)

    private val curves = mutableListOf<ChartDraw>()

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        shape.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        curves.forEach {
            it.draw(canvas)
        }
    }

    fun add(vararg draw: ChartDraw) {
        curves.addAll(draw)
    }

    fun clear() {
        curves.clear()
    }
}
