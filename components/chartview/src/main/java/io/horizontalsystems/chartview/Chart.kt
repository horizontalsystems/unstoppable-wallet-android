package io.horizontalsystems.chartview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.helpers.GridHelper
import io.horizontalsystems.chartview.helpers.PointHelper
import io.horizontalsystems.chartview.helpers.VolumeHelper
import io.horizontalsystems.chartview.models.ChartConfig
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.views.showIf
import kotlinx.android.synthetic.main.view_chart.view.*
import java.math.BigDecimal

interface ChartDraw {
    fun draw(canvas: Canvas)
}

class Chart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTouchDown()
        fun onTouchUp()
        fun onTouchSelect(point: ChartPoint)
    }

    interface RateFormatter {
        fun format(value: BigDecimal): String?
    }

    init {
        inflate(context, R.layout.view_chart, this)
    }

    private var rateFormatter: RateFormatter? = null

    private val config = ChartConfig(context, attrs)
    private val animator = ChartAnimator {
        chartMain.invalidate()
        chartBottom.invalidate()
        chartTimeline.invalidate()
    }

    private val mainCurve = ChartCurve(config, animator)
    private val mainGradient = ChartGradient(animator)
    private val bottomVolume = ChartVolume(config, animator)
    private val gridMain = ChartGrid(config)
    private val gridBottom = ChartGrid(config)
    private val gridTopLow = ChartGridDash(config)
    private val gridTimeline = ChartGridTimeline(config)

    fun setListener(listener: Listener) {
        chartTouch.onUpdate(object : Listener {
            override fun onTouchDown() {
                mainCurve.setColor(config.curvePressedColor)
                mainGradient.setShader(config.curvePressedColor)
                chartMain.invalidate()
                listener.onTouchDown()
            }

            override fun onTouchUp() {
                mainCurve.setColor(config.curveColor)
                mainGradient.setShader(config.curveColor)
                chartMain.invalidate()
                listener.onTouchUp()
            }

            override fun onTouchSelect(point: ChartPoint) {
                listener.onTouchSelect(point)
            }
        })
    }

    fun setFormatter(formatter: RateFormatter) {
        rateFormatter = formatter
    }

    fun showSinner() {
        chartError.showIf(false)
        chartViewSpinner.showIf(true)
        loadingShade.showIf(true)
    }

    fun hideSinner() {
        chartViewSpinner.showIf(false)
        loadingShade.showIf(false)
    }

    fun showError(error: String) {
        chartMain.visibility = View.INVISIBLE
        chartError.visibility = View.VISIBLE
        chartError.text = error
    }

    fun setData(points: List<ChartPoint>, chartType: ChartView.ChartType, startTimestamp: Long, endTimestamp: Long) {
        config.setTrendColor(points.firstOrNull(), points.lastOrNull(), endTimestamp)

        val shapeMain = chartMain.shape
        val shapeBottom = chartBottom.shape
        val shapeTimeline = chartTimeline.shape

        val coordinates = PointHelper.mapPoints(points, startTimestamp, endTimestamp, shapeMain, config.curveVerticalOffset)
        val volumeBars = VolumeHelper.mapPoints(points, startTimestamp, endTimestamp, shapeBottom, config.volumeMaxHeightRatio)
        val columns = GridHelper.mapColumns(chartType, startTimestamp, endTimestamp, shapeMain.right)
        val (top, low) = PointHelper.getTopLow(coordinates)

        chartTouch.configure(config, shapeTimeline.bottom)
        chartTouch.set(coordinates)

        mainCurve.setShape(shapeMain)
        mainCurve.setPoints(coordinates)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(coordinates)
        mainGradient.setShape(shapeMain)
        mainGradient.setShader(config.curveColor)

        bottomVolume.setBars(volumeBars)
        bottomVolume.setShape(shapeBottom)

        gridMain.setShape(shapeMain)
        gridMain.set(columns)

        gridBottom.setShape(shapeBottom)
        gridBottom.set(columns)

        gridTopLow.setShape(shapeMain)
        gridTopLow.setValues(formatRate(top.point.value), formatRate(low.point.value))

        gridTimeline.setColumns(columns)
        gridTimeline.setShape(shapeTimeline)

        // ---------------------------
        // *********
        // ---------------------------

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient, gridMain, gridTopLow)

        chartBottom.clear()
        chartBottom.add(gridBottom, bottomVolume)

        chartTimeline.clear()
        chartTimeline.add(gridTimeline)

        animator.start()
    }

    private fun formatRate(value: Float): String {
        return rateFormatter?.format(value.toBigDecimal()) ?: value.toString()
    }
}
