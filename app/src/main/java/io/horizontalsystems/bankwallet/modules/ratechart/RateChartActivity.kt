package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.cryptonews.CryptoNewsFragment
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.ChartPoint
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

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.cryptoNews, CryptoNewsFragment(coinCode))
            commit()
        }

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

    override fun onTouchSelect(point: ChartPoint) {
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
                chart.visibility = View.VISIBLE
                chart.setData(item.chartData, item.chartType)
            }

            coinRateDiff.diff = item.diffValue
        })

        presenterView.showMarketInfo.observe(this, Observer { item ->
            coinRateLast.text = formatter.formatForRates(item.rateValue)

            coinMarketCap.text = if (item.marketCap.value > BigDecimal.ZERO) {
                val shortCapValue = shortenValue(item.marketCap.value)
                val marketCap = CurrencyValue(item.marketCap.currency, shortCapValue.first)
                formatter.format(marketCap, canUseLessSymbol = false) + " " + shortCapValue.second
            } else {
                getString(R.string.NotAvailable)
            }

            val shortVolumeValue = shortenValue(item.volume.value)
            val volume = CurrencyValue(item.volume.currency, shortVolumeValue.first)
            volumeValue.text = formatter.format(volume, canUseLessSymbol = false) + " " + shortVolumeValue.second

            circulationValue.text = if (item.supply.value > BigDecimal.ZERO) {
                formatter.format(item.supply.value, item.supply.coinCode, trimmable = true)
            } else {
                getString(R.string.NotAvailable)
            }

            totalSupplyValue.text = item.maxSupply?.let {
                formatter.format(it.value, it.coinCode, trimmable = true)
            } ?: run {
                getString(R.string.NotAvailable)
            }
        })

        presenterView.setSelectedPoint.observe(this, Observer { item ->
            pointInfoVolume.visibility = View.INVISIBLE
            pointInfoVolumeTitle.visibility = View.INVISIBLE
            pointInfoTime.visibility = View.INVISIBLE

            val date = Date(item.date * 1000)
            if (item.chartType == ChartType.DAILY || item.chartType == ChartType.WEEKLY) {
                pointInfoTime.visibility = View.VISIBLE
                pointInfoTime.text = DateHelper.getOnlyTime(date)
            }

            pointInfoDate.text = DateHelper.shortDate(date, far = "MM/dd/yy")
            pointInfoPrice.text = formatter.formatForRates(item.currencyValue, maxFraction = 8)

            item.volume?.let {
                pointInfoVolumeTitle.visibility = View.VISIBLE
                pointInfoVolume.visibility = View.VISIBLE
                pointInfoVolume.text = formatter.format(item.volume, trimmable = true)
            }
        })

        presenterView.showError.observe(this, Observer {
            chart.showError(getString(R.string.Charts_Error_NotAvailable))
        })
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

        ema.setOnClickListener {
            chart.showEma()
        }

        macd.setOnClickListener {
            chart.showMacd()
        }

        rsi.setOnClickListener {
            chart.showRsi()
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
}
