package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.Indicator.Candle
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig
import kotlinx.android.synthetic.main.view_chart_minimal.view.*

class ChartMinimal @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_chart_minimal, this)
    }

    private val config = ChartConfig(context, attrs)

    private val mainCurve = ChartCurve(config, isVisible = true)
    private val mainGradient = ChartGradient()


    fun setData(data: ChartData) {
        config.setTrendColor(data)

        val points = PointConverter.curveForMinimal(data.values(Candle), chartMain.shape, config.curveMinimalVerticalOffset)

        mainCurve.setShape(chartMain.shape)
        mainCurve.setPoints(points)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(points)
        mainGradient.setShape(chartMain.shape)
        mainGradient.setShader(config.curveGradient)

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient)

        chartMain.invalidate()
    }

}
