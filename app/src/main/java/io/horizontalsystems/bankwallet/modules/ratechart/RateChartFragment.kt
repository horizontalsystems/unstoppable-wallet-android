package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.ChartType
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import kotlinx.android.synthetic.main.view_bottom_sheet_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment(private val coin: Coin) : BottomSheetDialogFragment(), ChartView.Listener {

    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView
    private val formatter = App.numberFormatter

    override fun getTheme(): Int {
        return R.style.BottomDialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_bottom_sheet_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartView.listener = this
        chartView.setIndicator(chartViewIndicator)
        chartTitle.text = getString(R.string.Charts_Title, coin.title)
        closeButton.setOnClickListener { dismiss() }

        presenter = ViewModelProviders.of(this, RateChartModule.Factory(coin)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        observeData()
        bindActions()

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
            when (type) {
                ChartType.DAILY -> resetActions(button1D)
                ChartType.WEEKLY -> resetActions(button1W)
                ChartType.MONTHLY -> resetActions(button1M)
                ChartType.MONTHLY6 -> resetActions(button6M)
                ChartType.MONTHLY18 -> resetActions(button1Y)
            }
        })

        presenterView.showChart.observe(viewLifecycleOwner, Observer { item ->
            chartView.setData(item.chartData)

            val diffColor = if (item.diffValue < BigDecimal.ZERO)
                resources.getColor(R.color.red_warning) else
                resources.getColor(R.color.green_crypto)

            coinRateDiff.setTextColor(diffColor)
            coinRateDiff.text = "${String.format("%.2f", item.diffValue)}%"

            item.rateValue?.let { coinRateLast.text = formatter.format(it, canUseLessSymbol = false) }
            coinMarketCap.text = formatter.format(item.marketCap, shorten = true)
            coinRateHigh.text = formatter.format(item.highValue, canUseLessSymbol = false)
            coinRateLow.text = formatter.format(item.lowValue, canUseLessSymbol = false)
            setViewVisibility(marketCapWrap, isVisible = true)
        })

        presenterView.setSelectedPoint.observe(viewLifecycleOwner, Observer { (time, value) ->
            pointInfoPrice.text = formatter.format(value, canUseLessSymbol = false)
            pointInfoDate.text = DateHelper.formatDate(Date(time * 1000), "MMM d, yyyy 'at' hh:mm a")
        })

        presenterView.enableChartType.observe(viewLifecycleOwner, Observer {
            // todo: disable button
        })

        presenterView.showError.observe(viewLifecycleOwner, Observer {
            chartError.visibility = View.VISIBLE
            chartError.text = it.message
        })
    }

    // ChartView Listener

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

    override fun onTouchSelect(point: DataPoint) {
        presenter.onTouchSelect(point)
    }

    private fun bindActions() {
        button1D.setOnClickListener {
            presenter.onSelect(ChartType.DAILY)
            resetActions(it)
        }
        button1W.setOnClickListener {
            presenter.onSelect(ChartType.WEEKLY)
            resetActions(it)
        }
        button1M.setOnClickListener {
            presenter.onSelect(ChartType.MONTHLY)
            resetActions(it)
        }
        button6M.setOnClickListener {
            presenter.onSelect(ChartType.MONTHLY6)
            resetActions(it)
        }
        button1Y.setOnClickListener {
            presenter.onSelect(ChartType.MONTHLY18)
            resetActions(it)
        }
    }

    private fun resetActions(current: View) {
        listOf(button1D, button1W, button1M, button6M, button1Y).forEach { it.isActivated = false }
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
}
