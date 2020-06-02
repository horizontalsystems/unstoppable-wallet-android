package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.showIf
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.activity_rate_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartActivity : BaseActivity(), Chart.Listener {
    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView
    private lateinit var coinCode: String

    private val formatter = App.numberFormatter
    private var actions = mapOf<ChartType, View>()

    private var emaTrend = ChartInfoTrend.NEUTRAL
    private var macdTrend = ChartInfoTrend.NEUTRAL
    private var rsiTrend = ChartInfoTrend.NEUTRAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_chart)

        coinCode = intent.getStringExtra(ModuleField.COIN_CODE) ?: run {
            finish()
            return
        }

        toolbar.title = intent.getStringExtra(ModuleField.COIN_TITLE)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, RateChartModule.Factory(coinCode)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        chart.setListener(this)
        chart.rateFormatter = presenter.rateFormatter

        observeData()
        bindActions()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        presenter.viewDidLoad()
    }

    //  ChartView Listener

    override fun onTouchDown() {
        scroller.setScrollingEnabled(false)

        setViewVisibility(chartPointsInfo, isVisible = true)
        setViewVisibility(chartActions, isVisible = false)
    }

    override fun onTouchUp() {
        scroller.setScrollingEnabled(true)

        setViewVisibility(chartPointsInfo, isVisible = false)
        setViewVisibility(chartActions, isVisible = true)
    }

    override fun onTouchSelect(point: PointInfo) {
        presenter.onTouchSelect(point)
    }

    //  Private

    private fun observeData() {
        presenterView.showSpinner.observe(this, Observer {
            chart.showSinner()
        })

        presenterView.hideSpinner.observe(this, Observer {
            chart.hideSinner()
        })

        presenterView.setDefaultMode.observe(this, Observer { type ->
            actions[type]?.let { resetActions(it, setDefault = true) }
        })

        presenterView.showChartInfo.observe(this, Observer { item ->
            rootView.post {
                setViewVisibility(chart, emaChartIndicator, macdChartIndicator, rsiChartIndicator, isVisible = true)
                chart.setData(item.chartData, item.chartType)
            }

            emaTrend = item.emaTrend
            macdTrend = item.macdTrend
            rsiTrend = item.rsiTrend

            emaChartIndicator.bind(EMA, false, emaTrend)
            macdChartIndicator.bind(MACD, false, macdTrend)
            rsiChartIndicator.bind(RSI, false, rsiTrend)

            coinRateDiff.diff = item.diffValue
        })

        presenterView.showMarketInfo.observe(this, Observer { item ->
            coinRateLast.text = formatter.formatFiat(item.rateValue.value, item.rateValue.currency.symbol, 2, 4)

            coinMarketCap.text = if (item.marketCap.value > BigDecimal.ZERO) {
                val shortCapValue = shortenValue(item.marketCap.value)
                formatter.formatFiat(shortCapValue.first, item.marketCap.currency.symbol, 0, 1) + shortCapValue.second
            } else {
                getString(R.string.NotAvailable)
            }

            val shortVolumeValue = shortenValue(item.volume.value)
            volumeValue.text = formatter.formatFiat(shortVolumeValue.first, item.volume.currency.symbol, 0, 1) + shortVolumeValue.second

            circulationValue.text = if (item.supply.value > BigDecimal.ZERO) {
                val shortValue = shortenValue(item.supply.value)
                formatter.format(shortValue.first,0,1, suffix = "${shortValue.second} ${item.supply.coinCode}")
            } else {
                getString(R.string.NotAvailable)
            }

            totalSupplyValue.text = item.maxSupply?.let {
                val shortValue = shortenValue(it.value)
                formatter.format(shortValue.first,0,1, suffix = "${shortValue.second} ${it.coinCode}")
            } ?: run {
                getString(R.string.NotAvailable)
            }
        })

        presenterView.setSelectedPoint.observe(this, Observer { item ->
            pointInfoVolume.visibility = View.INVISIBLE
            pointInfoVolumeTitle.visibility = View.INVISIBLE

            macdHistogram.visibility = View.INVISIBLE
            macdSignal.visibility = View.INVISIBLE
            macdValue.visibility = View.INVISIBLE

            pointInfoDate.text = DateHelper.getDayAndTime(Date(item.date * 1000))
            pointInfoPrice.text = formatter.formatFiat(item.price.value, item.price.currency.symbol, 2, 4)

            item.volume?.let {
                pointInfoVolumeTitle.visibility = View.VISIBLE
                pointInfoVolume.visibility = View.VISIBLE
                pointInfoVolume.text = formatter.formatFiat(item.volume.value, item.volume.currency.symbol, 0, 2)
            }

            item.macdInfo?.let {macdInfo ->
                macdInfo.histogram?.let {
                    macdHistogram.visibility = View.VISIBLE
                    macdHistogram.setTextColor(getHistogramColor(it))
                    macdHistogram.text = formatter.format(it, 0, 2)
                }
                macdInfo.signal?.let {
                    macdSignal.visibility = View.VISIBLE
                    macdSignal.text = formatter.format(it, 0, 2)
                }
                macdInfo.macd?.let {
                    macdValue.visibility = View.VISIBLE
                    macdValue.text = formatter.format(it, 0, 2)
                }
            }
        })

        presenterView.showError.observe(this, Observer {
            chart.showError(getString(R.string.Charts_Error_NotAvailable))
        })

        presenterView.showEma.observe(this, Observer { enabled ->
            chart.showEma(enabled)
            emaChartIndicator.bind(EMA, enabled, emaTrend)
        })

        presenterView.showMacd.observe(this, Observer { enabled ->
            chart.showMacd(enabled)
            macdChartIndicator.bind(MACD, enabled, macdTrend)

            setViewVisibility(pointInfoVolume, pointInfoVolumeTitle, isVisible = !enabled)
            setViewVisibility(macdSignal,macdHistogram, macdValue, isVisible = enabled)
        })

        presenterView.showRsi.observe(this, Observer { enabled ->
            chart.showRsi(enabled)
            rsiChartIndicator.bind(RSI, enabled, rsiTrend)
        })

    }

    private fun getHistogramColor(value: Float): Int {
        val textColor = if (value > 0) R.color.green_d else R.color.red_d
        return getColor(textColor)
    }

    private fun bindActions() {
        actions = mapOf(
                Pair(ChartType.DAILY, button1D),
                Pair(ChartType.WEEKLY, button1W),
                Pair(ChartType.MONTHLY, button1M),
                Pair(ChartType.MONTHLY3, button3M),
                Pair(ChartType.MONTHLY6, button6M),
                Pair(ChartType.MONTHLY12, button1Y),
                Pair(ChartType.MONTHLY24, button2Y)
        )

        actions.forEach { (type, action) ->
            action.setOnClickListener { view ->
                presenter.onSelect(type)
                resetActions(view)
            }
        }

        emaChartIndicator.setOnClickListener {
            presenter.toggleEma()
        }

        macdChartIndicator.setOnClickListener {
            presenter.toggleMacd()
        }

        rsiChartIndicator.setOnClickListener {
            presenter.toggleRsi()
        }
    }

    private fun resetActions(current: View, setDefault: Boolean = false) {
        actions.values.forEach { it.isActivated = false }
        current.isActivated = true

        val inLeftSide = chart.width / 2 < current.left
        if (setDefault) {
            chartActionsWrap.scrollTo(if (inLeftSide) chart.width else 0, 0)
            return
        }

        val by = if (inLeftSide) {
            chart.scrollX + current.width
        } else {
            chart.scrollX - current.width
        }

        chartActionsWrap.smoothScrollBy(by, 0)
    }

    private fun setViewVisibility(vararg views: View, isVisible: Boolean) {
        views.forEach { it.showIf(isVisible, hideType = View.INVISIBLE) }
    }

    // Need to move this to helpers

    private fun shortenValue(number: Number): Pair<BigDecimal, String> {
        val suffix = arrayOf(
                " ",
                getString(R.string.Charts_MarketCap_Thousand),
                getString(R.string.Charts_MarketCap_Million),
                getString(R.string.Charts_MarketCap_Billion),
                getString(R.string.Charts_MarketCap_Trillion)) // "P", "E"

        val valueLong = number.toLong()
        val value = Math.floor(Math.log10(valueLong.toDouble())).toInt()
        val base = value / 3

        var returnSuffix = ""
        var valueDecimal = valueLong.toBigDecimal()
        if (value >= 3 && base < suffix.size) {
            valueDecimal = (valueLong / Math.pow(10.0, (base * 3).toDouble())).toBigDecimal()
            returnSuffix = suffix[base]
        }

        return Pair(valueDecimal, returnSuffix)
    }

    companion object{
        private const val EMA = "EMA"
        private const val MACD = "MACD"
        private const val RSI = "RSI"
    }
}
