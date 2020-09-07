package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.activity_rate_chart.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*


class RateChartActivity : BaseActivity(), Chart.Listener {
    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView

    private val formatter = App.numberFormatter
    private var actions = mapOf<ChartType, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rate_chart)

        val coinId = intent.getStringExtra(ModuleField.COIN_ID)

        val coinCode = intent.getStringExtra(ModuleField.COIN_CODE) ?: run {
            finish()
            return
        }

        val coinTitle = intent.getStringExtra(ModuleField.COIN_TITLE) ?: ""
        toolbar.title = coinTitle
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = ViewModelProvider(this, RateChartModule.Factory(coinTitle, coinCode, coinId)).get(RateChartPresenter::class.java)
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.rate_chart_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menuNotification)?.apply {
            isVisible = presenter.notificationIconVisible
            val iconRes = if (presenter.notificationIconActive) R.drawable.ic_notification_24 else R.drawable.ic_notification_inactive_24
            icon = ContextCompat.getDrawable(this@RateChartActivity, iconRes)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuNotification -> {
                presenter.onNotificationClick()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
                chart.showChart()
                chart.setData(item.chartData, item.chartType)
            }

            emaChartIndicator.bind(item.emaTrend)
            macdChartIndicator.bind(item.macdTrend)
            rsiChartIndicator.bind(item.rsiTrend)

            coinRateDiff.diff = item.diffValue
        })

        presenterView.showMarketInfo.observe(this, Observer { item ->
            coinRateLast.text = formatter.formatFiat(item.rateValue.value, item.rateValue.currency.symbol, 2, 4)

            coinMarketCap.apply {
                if (item.marketCap.value > BigDecimal.ZERO) {
                    text = formatFiatShortened(item.marketCap.value, item.marketCap.currency.symbol)
                } else {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            volumeValue.apply {
                text = formatFiatShortened(item.volume.value, item.volume.currency.symbol)
            }

            circulationValue.apply {
                if (item.supply.value > BigDecimal.ZERO) {
                    text = formatter.formatCoin(item.supply.value, item.supply.coinCode, 0, 2)
                } else {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            totalSupplyValue.apply {
                item.maxSupply?.let {
                    text = formatter.formatCoin(it.value, it.coinCode, 0, 2)
                } ?: run {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            startDateValue.apply {
                text = item.startDate ?: getString(R.string.NotAvailable)
                isEnabled = !item.startDate.isNullOrEmpty()
            }

            websiteValue.apply {
                item.website?.let {
                    text = TextHelper.getUrl(it)
                } ?: run {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

        })

        presenterView.setSelectedPoint.observe(this, Observer { item ->
            pointInfoVolume.isInvisible = true
            pointInfoVolumeTitle.isInvisible = true

            macdHistogram.isInvisible = true
            macdSignal.isInvisible = true
            macdValue.isInvisible = true

            pointInfoDate.text = DateHelper.getDayAndTime(Date(item.date * 1000))
            pointInfoPrice.text = formatter.formatFiat(item.price.value, item.price.currency.symbol, 2, 4)

            item.volume?.let {
                pointInfoVolumeTitle.isVisible = true
                pointInfoVolume.isVisible = true
                pointInfoVolume.text = formatFiatShortened(item.volume.value, item.volume.currency.symbol)
            }

            item.macdInfo?.let { macdInfo ->
                macdInfo.histogram?.let {
                    macdHistogram.isVisible = true
                    macdHistogram.setTextColor(getHistogramColor(it))
                    macdHistogram.text = formatter.format(it, 0, 2)
                }
                macdInfo.signal?.let {
                    macdSignal.isVisible = true
                    macdSignal.text = formatter.format(it, 0, 2)
                }
                macdInfo.macd?.let {
                    macdValue.isVisible = true
                    macdValue.text = formatter.format(it, 0, 2)
                }
            }
        })

        presenterView.showError.observe(this, Observer {
            chart.showError(getString(R.string.Charts_Error_NotAvailable))
        })

        presenterView.showEma.observe(this, Observer { enabled ->
            chart.showEma(enabled)
            emaChartIndicator.setStateEnabled(enabled)
        })

        presenterView.showMacd.observe(this, Observer { enabled ->
            chart.showMacd(enabled)
            macdChartIndicator.setStateEnabled(enabled)

            setViewVisibility(pointInfoVolume, pointInfoVolumeTitle, isVisible = !enabled)
            setViewVisibility(macdSignal, macdHistogram, macdValue, isVisible = enabled)
        })

        presenterView.showRsi.observe(this, Observer { enabled ->
            chart.showRsi(enabled)
            rsiChartIndicator.setStateEnabled(enabled)
        })

        presenterView.alertNotificationUpdated.observe(this, Observer { visible ->
            invalidateOptionsMenu()
        })

        presenterView.showNotificationMenu.observe(this, Observer { (coinId, coinName) ->
            BottomNotificationMenu.show(supportFragmentManager, NotificationMenuMode.All, coinName, coinId)
        })

    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = shortenValue(value)
        return formatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }

    private fun getHistogramColor(value: Float): Int {
        val textColor = if (value > 0) R.color.green_d else R.color.red_d
        return getColor(textColor)
    }

    private fun bindActions() {
        actions = mapOf(
                Pair(ChartType.DAILY, button1D),
                Pair(ChartType.WEEKLY, button1W),
                Pair(ChartType.WEEKLY2, button2W),
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
        views.forEach { it.isInvisible = !isVisible }
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

        val roundedDecimalValue = if (valueDecimal < BigDecimal.TEN) {
            valueDecimal.setScale(2, RoundingMode.HALF_EVEN)
        } else {
            valueDecimal.setScale(1, RoundingMode.HALF_EVEN)
        }

        return Pair(roundedDecimalValue, returnSuffix)
    }

}
