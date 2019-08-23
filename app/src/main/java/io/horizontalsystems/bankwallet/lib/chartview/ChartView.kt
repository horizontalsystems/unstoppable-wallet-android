package io.horizontalsystems.bankwallet.lib.chartview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartData
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint

class ChartView : View {
    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchMove(point: DataPoint)
    }

    enum class Mode {
        DAILY,
        WEEKLY,
        MONTHLY,
        MONTHLY6,
        MONTHLY18
    }

    var listener: Listener? = null

    private val viewHelper = ViewHelper(context)
    private val scaleHelper = ScaleHelper()

    private val shape = RectF()
    private val chart = Chart(context, shape)
    private val chartGrid = ChartGrid(context, shape)
    private var chartIndicator: ChartViewIndicator? = null

    private var animatingFraction = 0f

    // Animator
    private val animator = ValueAnimator().apply {
        interpolator = AccelerateInterpolator()
        duration = 500
        addUpdateListener { animator ->
            // Get our float from the animation. This method returns the Interpolated float.
            animatingFraction = animator.animatedFraction
            invalidate()
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        chart.draw(canvas, animatingFraction)
        chartGrid.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                chart.onTouchActive()
                listener?.onTouchDown()
                chartIndicator?.onMove(chart.findPoint(event.rawX), listener)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                chartIndicator?.onMove(chart.findPoint(event.rawX), listener)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                chart.onTouchInactive()
                listener?.onTouchUp()
                invalidate()
            }
        }

        return true
    }

    fun setIndicator(indicator: ChartViewIndicator) {
        this.chartIndicator = indicator
    }

    fun setData(data: ChartData) {
        setPoints(data)

        animator.setFloatValues(0f)
        animator.start()
    }

    private fun setPoints(data: ChartData) {
        val min = data.points.min() ?: 0f
        val max = data.points.max() ?: 0f

        val (valueTop, valueStep) = scaleHelper.scale(min, max)
        val valueWidth = viewHelper.measureTextWidth(valueTop.toString())

        shape.set(0f, 0f, width - valueWidth, height - viewHelper.dp2px(20f))

        chart.init(data, valueTop, valueStep)
        chartGrid.init(data, valueTop, valueStep, valueWidth)
    }
}
