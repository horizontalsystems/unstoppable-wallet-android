package io.horizontalsystems.chartview

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.chartview.Indicator.Candle
import io.horizontalsystems.chartview.helpers.ChartAnimator
import io.horizontalsystems.chartview.helpers.PointConverter
import io.horizontalsystems.chartview.models.ChartConfig
import kotlinx.android.synthetic.main.view_chart_minimal.view.*

class ChartMinimal @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_chart_minimal, this)
    }

    private val config = ChartConfig(context, attrs)
    private val animatorMain = ChartAnimator { chartMain.invalidate() }

    private val mainCurve = ChartCurve(config, animatorMain, isVisible = true)
    private val mainGradient = ChartGradient(animatorMain)


    fun setData(data: ChartData) {
        config.setTrendColor(data)

        val points = PointConverter.curve(data.values(Candle), chartMain.shape, config.curveVerticalOffset)

        mainCurve.setShape(chartMain.shape)
        mainCurve.setPoints(points)
        mainCurve.setColor(config.curveColor)

        mainGradient.setPoints(points)
        mainGradient.setShape(chartMain.shape)
        mainGradient.setShader(config.curveColor)

        chartMain.clear()
        chartMain.add(mainCurve, mainGradient)

        animatorMain.start()
    }

}
