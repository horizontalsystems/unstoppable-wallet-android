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
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.lib.chartview.ChartView
import io.horizontalsystems.bankwallet.lib.chartview.ChartView.Mode
import io.horizontalsystems.bankwallet.lib.chartview.models.DataPoint
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import kotlinx.android.synthetic.main.view_bottom_sheet_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment(private val coin: Coin, private val rate: Rate?) : BottomSheetDialogFragment(), ChartView.Listener {

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

        presenter = ViewModelProviders.of(this, RateChartModule.Factory(coin)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        initialize()
        observeData()
        bindActions()

        presenter.viewDidLoad()
    }

    private fun initialize() {
        chartView.listener = this
        chartView.setIndicator(chartViewIndicator)
        chartTitle.text = getString(R.string.Charts_Title, coin.title)

        if (rate == null) return
        val currencyValue = CurrencyValue(presenter.currency, rate.value)
        coinRateLast.text = formatter.format(currencyValue)
    }

    private fun observeData() {
        presenterView.showSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, isVisible = false)
            setViewVisibility(chartViewSpinner, isVisible = true)
        })

        presenterView.hideSpinner.observe(viewLifecycleOwner, Observer {
            setViewVisibility(chartView, isVisible = true)
            setViewVisibility(chartViewSpinner, isVisible = false)
        })

        presenterView.setDefaultMode.observe(viewLifecycleOwner, Observer { mode ->
            updateLabels(mode)
            when (mode) {
                Mode.DAILY -> resetActions(button1D)
                Mode.WEEKLY -> resetActions(button1W)
                Mode.MONTHLY -> resetActions(button1M)
                Mode.MONTHLY6 -> resetActions(button6M)
                Mode.MONTHLY18 -> resetActions(button1Y)
            }
        })

        presenterView.showRate.observe(viewLifecycleOwner, Observer { (lastRate, startRate) ->
            val diffColour: Int
            val diffString: String

            val rateDiff = -(startRate - lastRate)
            if (rateDiff < BigDecimal.ZERO) {
                diffString = String.format("%.2f", rateDiff * BigDecimal(100) / startRate) + " %"
                diffColour = resources.getColor(R.color.red_warning)
            } else {
                diffString = String.format("%.2f", rateDiff * BigDecimal(100) / lastRate) + " %"
                diffColour = resources.getColor(R.color.green_crypto)
            }

            coinRateDiff.setTextColor(diffColour)
            coinRateDiff.text = diffString
            coinRateLast.text = formatter.format(CurrencyValue(presenter.currency, lastRate))
        })

        presenterView.showMarketCap.observe(viewLifecycleOwner, Observer {
            coinMarketCap.text = formatter.format(CurrencyValue(presenter.currency, it.first), shorten = true)
            coinRateHigh.text = formatter.format(CurrencyValue(presenter.currency, it.second), shorten = true)
            coinRateLow.text = formatter.format(CurrencyValue(presenter.currency, it.third), shorten = true)
            setViewVisibility(marketCapWrap, isVisible = true)
        })

        presenterView.showChart.observe(viewLifecycleOwner, Observer {
            chartView.setData(it)
            updateLabels(it.mode)
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

    override fun onTouchMove(point: DataPoint) {
        pointInfoPrice.text = point.value.toString()
        pointInfoDate.text = DateHelper.formatDate(Date(point.time * 1000), "MMM d, yyyy 'at' hh:mm a")
    }

    private fun bindActions() {
        button1D.setOnClickListener {
            presenter.onClick(Mode.DAILY)
            resetActions(it)
        }
        button1W.setOnClickListener {
            presenter.onClick(Mode.WEEKLY)
            resetActions(it)
        }
        button1M.setOnClickListener {
            presenter.onClick(Mode.MONTHLY)
            resetActions(it)
        }
        button6M.setOnClickListener {
            presenter.onClick(Mode.MONTHLY6)
            resetActions(it)
        }
        button1Y.setOnClickListener {
            presenter.onClick(Mode.MONTHLY18)
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

    private fun updateLabels(mode: Mode) {
        val modeName = when (mode) {
            Mode.DAILY -> getString(R.string.Charts_TimeDuration_Day)
            Mode.WEEKLY -> getString(R.string.Charts_TimeDuration_Week)
            Mode.MONTHLY -> getString(R.string.Charts_TimeDuration_Month)
            Mode.MONTHLY6 -> getString(R.string.Charts_TimeDuration_HalfYear)
            Mode.MONTHLY18 -> getString(R.string.Charts_TimeDuration_Year)
        }

        coinRateHighTitle.text = getString(R.string.Charts_Rate_High, modeName)
        coinRateLowTitle.text = getString(R.string.Charts_Rate_Low, modeName)
    }
}
