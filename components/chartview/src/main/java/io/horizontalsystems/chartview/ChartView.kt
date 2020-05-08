package io.horizontalsystems.chartview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint
import java.math.BigDecimal

class ChartView : View {

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(point: ChartPoint)
    }

    interface RateFormatter {
        fun format(value: BigDecimal): String?
    }

    enum class ChartType {
        DAILY,
        WEEKLY,
        MONTHLY,
        MONTHLY3,
        MONTHLY6,
        MONTHLY12,
        MONTHLY24;
    }

    var listener: Listener? = null

    private val config = ChartConfig(context)
    private val helper = ScaleHelper(config)

    private val shape = RectF()
    private val chartCurve = ChartCurve(shape, config)
    private val chartGrid = ChartGrid(shape, config)
    private val chartVolume = ChartVolume(config, shape)
    private var chartCursor: ChartCursor? = null
    private var postponedJob: Runnable? = null

    private var animator: ValueAnimator? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ChartView)
        try {
            ta.getBoolean(R.styleable.ChartView_showGrid, true).let { config.showGrid = it }
            ta.getBoolean(R.styleable.ChartView_animated, true).let { config.animated = it }
            ta.getDimension(R.styleable.ChartView_width, 0f).let { config.width = it }
            ta.getDimension(R.styleable.ChartView_height, 0f).let { config.height = it }
            ta.getInt(R.styleable.ChartView_growColor, context.getColor(R.color.green_d)).let { config.growColor = it }
            ta.getInt(R.styleable.ChartView_fallColor, context.getColor(R.color.red_d)).let { config.fallColor = it }
            ta.getInt(R.styleable.ChartView_textColor, context.getColor(R.color.grey)).let { config.textColor = it }
            ta.getInt(R.styleable.ChartView_textPriceColor, context.getColor(R.color.light_grey)).let { config.textPriceColor = it }
            ta.getInt(R.styleable.ChartView_gridColor, context.getColor(R.color.steel_20)).let { config.gridColor = it }
            ta.getInt(R.styleable.ChartView_touchColor, context.getColor(R.color.light)).let { config.touchColor = it }
            ta.getInt(R.styleable.ChartView_cursorColor, context.getColor(R.color.light)).let { config.cursorColor = it }
            ta.getInt(R.styleable.ChartView_gridDottedColor, context.getColor(R.color.white_50)).let { config.gridDottedColor = it }
            ta.getInt(R.styleable.ChartView_partialChartColor, context.getColor(R.color.light)).let { config.partialChartColor = it }
        } finally {
            ta.recycle()
        }
    }

    override fun willNotDraw(): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        chartCurve.draw(canvas)
        chartVolume.draw(canvas)
        if (config.showGrid) {
            chartGrid.draw(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventListener = listener ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                chartCurve.onTouchActive()
                eventListener.onTouchDown()
                chartCursor?.onMove(chartCurve.find(event.x), eventListener)
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                chartCursor?.onMove(chartCurve.find(event.x), eventListener)
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                chartCurve.onTouchInactive()
                eventListener.onTouchUp()
                invalidate()
            }
        }

        return true
    }

    fun setFormatter(formatter: RateFormatter) {
        chartCurve.formatter = formatter
    }

    fun setCursor(cursor: ChartCursor) {
        chartCursor = cursor
        chartCursor?.init(config)
    }

    fun onNoChart() {
        animator = null
    }

    fun setData(points: List<ChartPoint>, chartType: ChartType, startTimestamp: Long, endTimestamp: Long) {
        if (animator != null) {
            postponedJob = Runnable {
                startAnimation(points, endTimestamp, chartType, startTimestamp)
                postponedJob = null
            }
            animator?.reverse()
        } else {
            initAnimator()
            startAnimation(points, endTimestamp, chartType, startTimestamp)
        }
    }

    private fun initAnimator() {
        animator = ValueAnimator().apply {
            interpolator = AccelerateInterpolator()
            duration = 300
            addUpdateListener { animator ->
                // Get our float from the animation. This method returns the Interpolated float.
                config.animatedFraction = animator.animatedFraction
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    postponedJob?.run()
                }
            })
        }
    }

    private fun startAnimation(points: List<ChartPoint>, endTimestamp: Long, chartType: ChartType, startTimestamp: Long) {
        setColour(points, endTimestamp)
        setPoints(points, chartType, startTimestamp, endTimestamp)

        animator?.setFloatValues(0f)
        animator?.start()
    }

    private fun setColour(points: List<ChartPoint>, endTimestamp: Long) {
        val startPoint = points.firstOrNull() ?: return
        val endPoint = points.lastOrNull() ?: return

        if (endPoint.timestamp < endTimestamp) {
            config.curveColor = config.partialChartColor
        } else if (startPoint.value > endPoint.value) {
            config.curveColor = config.fallColor
        } else {
            config.curveColor = config.growColor
        }
    }

    private fun setPoints(points: List<ChartPoint>, chartType: ChartType, startTimestamp: Long, endTimestamp: Long) {
        helper.scale(points)

        var shapeWidth = width.toFloat()
        if (shapeWidth == 0f) {
            shapeWidth = config.width
        }

        var shapeHeight = height.toFloat()
        if (shapeHeight == 0f) {
            shapeHeight = config.height
        }

        if (config.showGrid) {
            config.offsetBottom = config.dp2px(20f)
        }

        shape.set(0f, 0f, shapeWidth, shapeHeight - config.offsetBottom)

        chartCurve.init(points, startTimestamp, endTimestamp)

        chartVolume.init(points, startTimestamp, endTimestamp)

        if (config.showGrid) {
            chartGrid.init(chartType, startTimestamp, endTimestamp)
        }
    }
}
