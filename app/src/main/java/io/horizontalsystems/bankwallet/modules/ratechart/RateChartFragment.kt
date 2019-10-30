package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.models.ChartPoint
import io.horizontalsystems.bankwallet.ui.extensions.BaseBottomSheetDialogFragment
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.view_bottom_sheet_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment(private val coin: Coin) : BaseBottomSheetDialogFragment(), ChartView.Listener {

    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView

    private val formatter = App.numberFormatter
    private var actions = mapOf<ChartType, View>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setContentView(R.layout.view_bottom_sheet_chart)

        setTitle(getString(R.string.Charts_Title, coin.title))
        setHeaderIcon(LayoutHelper.getCoinDrawableResource(coin.code))

        chartView.listener = this
        chartView.setIndicator(chartViewIndicator)

        presenter = ViewModelProviders.of(this, RateChartModule.Factory(coin)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        observeData()
        bindActions()
    }

    override fun onShow() {
        presenter.viewDidLoad()
    }

    private fun observeData() {
        presenterView.showSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, chartError, isVisible = false)
            setViewVisibility(chartViewSpinner, isVisible = true)
        })

        presenterView.hideSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, isVisible = true)
            setViewVisibility(chartViewSpinner, isVisible = false)
        })

        presenterView.setDefaultMode.observe(viewLifecycleOwner, Observer { type ->
            actions[type]?.let { it.isActivated = true }
        })

        presenterView.showChart.observe(viewLifecycleOwner, Observer { item ->
            chartView.visibility = View.VISIBLE
            chartView.setData(item.chartPoints, item.chartType, item.startTimestamp, item.endTimestamp)

            val diffColor = if (item.diffValue < BigDecimal.ZERO)
                resources.getColor(R.color.red_d) else
                resources.getColor(R.color.green_d)

            coinRateDiff.setTextColor(diffColor)
            coinRateDiff.text = App.numberFormatter.format(item.diffValue.toDouble(), showSign = true, precision = 2) + "%"

            item.rateValue?.let { coinRateLast.text = formatter.format(it, canUseLessSymbol = false) }
            val shortValue = shortenValue(item.marketCap.value)
            val marketCap = CurrencyValue(item.marketCap.currency, shortValue.first)
            coinMarketCap.text = formatter.format(marketCap, canUseLessSymbol = false) + shortValue.second

            coinRateHigh.text = formatter.format(item.highValue, canUseLessSymbol = false)
            coinRateLow.text = formatter.format(item.lowValue, canUseLessSymbol = false)
            setViewVisibility(marketCapWrap, isVisible = true)
        })

        presenterView.setSelectedPoint.observe(viewLifecycleOwner, Observer { (time, value, type) ->
            val outputFormat = when (type) {
                ChartType.DAILY,
                ChartType.WEEKLY -> "MMM d, yyyy 'at' HH:mm a"
                else -> "MMM d, yyyy"
            }
            pointInfoPrice.text = formatter.format(value, canUseLessSymbol = false)
            pointInfoDate.text = DateHelper.formatDate(Date(time * 1000), outputFormat)
        })

        presenterView.showError.observe(viewLifecycleOwner, Observer {
            chartView.visibility = View.INVISIBLE
            chartError.visibility = View.VISIBLE
            chartError.text = getString(R.string.Charts_Error_NotAvailable)
        })
    }

    //  ChartView Listener

    override fun onTouchDown() {
        isCancelable = false // enable swipe

        setViewVisibility(chartPointsInfo, chartViewIndicator, isVisible = true)
        setViewVisibility(chartActions, isVisible = false)
    }

    override fun onTouchUp() {
        isCancelable = true // enable swipe

        setViewVisibility(chartPointsInfo, chartViewIndicator, isVisible = false)
        setViewVisibility(chartActions, isVisible = true)
    }

    override fun onTouchSelect(point: ChartPoint) {
        presenter.onTouchSelect(point)
    }

    private fun bindActions() {
        actions = mapOf(
                Pair(ChartType.DAILY, button1D),
                Pair(ChartType.WEEKLY, button1W),
                Pair(ChartType.MONTHLY, button1M),
                Pair(ChartType.MONTHLY6, button6M),
                Pair(ChartType.MONTHLY12, button1Y)
        )

        actions.forEach { (type, action) ->
            action.setOnClickListener {
                presenter.onSelect(type)
                resetActions(it)
            }
        }
    }

    private fun resetActions(current: View) {
        actions.values.forEach { it.isActivated = false }
        setViewVisibility(marketCapWrap, isVisible = false)
        current.isActivated = true
    }

    private fun setViewVisibility(vararg views: View, isVisible: Boolean) {
        views.forEach {
            if (isVisible)
                it.visibility = View.VISIBLE else
                it.visibility = View.INVISIBLE
        }
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
