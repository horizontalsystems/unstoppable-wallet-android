package io.horizontalsystems.walletkit.modules.coin.overview.ui

import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartTransition
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.ChartVolume
import io.horizontalsystems.designkit.theme.lightPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ChartHelperTest {

    private val line = linkedMapOf(0L to 1f, 100L to 3f)
    private val movingAverage = "ma" to ChartIndicator.MovingAverage(line, 0xFFFFA800)
    private val rsi = "rsi" to ChartIndicator.Rsi(linkedMapOf(0L to 40f, 100L to 60f))
    private val macd = "macd" to ChartIndicator.Macd(line, line, line)

    private fun data(vararg indicators: Pair<String, ChartIndicator>) = ChartData(
        items = listOf(
            ChartPoint(1f, 0, ChartVolume(5f)),
            ChartPoint(3f, 100, ChartVolume(7f)),
        ),
        isMovementChart = true,
        indicators = mapOf(*indicators),
    )

    /** A chart that has finished appearing with [indicators] on it. */
    private fun shownChart(vararg indicators: Pair<String, ChartIndicator>): ChartHelper {
        val chartData = data(*indicators)
        return ChartHelper(
            target = chartData,
            hasVolumes = true,
            colors = lightPalette,
            transition = ChartTransition.Morph,
            appear = ChartTransition.Rise,
            show = ChartTransition.FadeIn(),
            hide = ChartTransition.FadeOut(),
        ).apply {
            setTarget(chartData, true)
            onNextFrame(1f)
        }
    }

    private fun ChartHelper.change(vararg indicators: Pair<String, ChartIndicator>) =
        setTarget(data(*indicators), true)

    private fun ChartHelper.movingAverageAlphas() = getMovingAverageCurveStates().map { it.alpha }

    @Test
    fun aHiddenMovingAverage_fadesOutAndIsDroppedWhenTheAnimationEnds() {
        val chart = shownChart(movingAverage)

        chart.change()
        chart.onNextFrame(0.5f)
        assertEquals(listOf(0.5f), chart.movingAverageAlphas())

        chart.onNextFrame(1f)
        assertEquals(emptyList<Float>(), chart.movingAverageAlphas())
    }

    @Test
    fun aRefreshDuringAHide_goesOnFadingFromTheOpacityOnScreen() {
        val chart = shownChart(movingAverage)
        chart.change()
        chart.onNextFrame(0.5f)

        chart.change()

        assertEquals(listOf(0.5f), chart.movingAverageAlphas())
        chart.onNextFrame(0.5f)
        assertEquals(listOf(0.25f), chart.movingAverageAlphas())
        chart.onNextFrame(1f)
        assertEquals(emptyList<Float>(), chart.movingAverageAlphas())
    }

    @Test
    fun aMovingAverageThatReturnsDuringItsHide_comesBackAsOneCurveFromItsOpacity() {
        val chart = shownChart(movingAverage)
        chart.change()
        chart.onNextFrame(0.5f)

        chart.change(movingAverage)

        assertEquals(listOf(0.5f), chart.movingAverageAlphas())
        chart.onNextFrame(0.5f)
        assertEquals(listOf(0.75f), chart.movingAverageAlphas())
        chart.onNextFrame(1f)
        assertEquals(listOf(1f), chart.movingAverageAlphas())
    }

    @Test
    fun aMovingAverageSwitchedOnLater_fadesIn() {
        val chart = shownChart()

        chart.change(movingAverage)

        assertEquals(listOf(0f), chart.movingAverageAlphas())
        chart.onNextFrame(0.5f)
        assertEquals(listOf(0.5f), chart.movingAverageAlphas())
    }

    @Test
    fun volumeBars_crossFadeWithAnIndicatorThatTakesTheirPanel() {
        val chart = shownChart()

        chart.change(rsi)
        chart.onNextFrame(0.5f)
        assertEquals(0.5f, chart.getVolumeBarsState()?.alpha)
        assertEquals(0.5f, chart.getRsiCurveState()?.alpha)
        chart.onNextFrame(1f)
        assertNull(chart.getVolumeBarsState())

        chart.change()
        assertEquals(0f, chart.getVolumeBarsState()?.alpha)
        chart.onNextFrame(0.5f)
        assertEquals(0.5f, chart.getVolumeBarsState()?.alpha)
        assertEquals(0.5f, chart.getRsiCurveState()?.alpha)
        chart.onNextFrame(1f)
        assertEquals(1f, chart.getVolumeBarsState()?.alpha)
        assertNull(chart.getRsiCurveState())
    }

    @Test
    fun volumeBars_comeBackFromTheirOpacityWhenTheIndicatorGoesMidHide() {
        val chart = shownChart()
        chart.change(rsi)
        chart.onNextFrame(0.5f)

        chart.change()

        assertEquals(0.5f, chart.getVolumeBarsState()?.alpha)
        chart.onNextFrame(1f)
        assertEquals(1f, chart.getVolumeBarsState()?.alpha)
    }

    @Test
    fun aLeavingMacd_staysDrawnWhileRsiTakesThePanel() {
        val chart = shownChart(macd)

        chart.change(rsi)
        chart.onNextFrame(0.5f)

        assertEquals(0.5f, chart.getMacdLineCurveState()?.alpha)
        assertEquals(0.5f, chart.getRsiCurveState()?.alpha)
        chart.onNextFrame(1f)
        assertNull(chart.getMacdLineCurveState())
        assertNotNull(chart.getRsiCurveState())
    }

    @Test
    fun rsi_hasThePanelWhenMacdIsInTheDataToo() {
        val chart = shownChart(rsi, macd)

        assertNotNull(chart.getRsiCurveState())
        assertNull(chart.getMacdLineCurveState())
    }

    @Test
    fun aMacdBehindRsi_isNotDrawnWhenItIsTakenAway() {
        val chart = shownChart(rsi, macd)

        chart.change(rsi)

        assertNull(chart.getMacdLineCurveState())
        chart.onNextFrame(0.5f)
        assertNull(chart.getMacdLineCurveState())
        assertEquals(1f, chart.getRsiCurveState()?.alpha)
    }
}
