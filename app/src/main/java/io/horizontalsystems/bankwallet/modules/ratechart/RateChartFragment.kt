package io.horizontalsystems.bankwallet.modules.ratechart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.fragment_rate_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment : BaseFragment(), Chart.Listener {
    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView

    private val formatter = App.numberFormatter
    private var actions = mapOf<ChartType, View>()
    private var notificationMenuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_rate_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val coinId = arguments?.getString(COIN_ID_KEY)

        val coinCode = arguments?.getString(COIN_CODE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        val coinTitle = arguments?.getString(COIN_TITLE_KEY) ?: ""

        presenter = ViewModelProvider(this, RateChartModule.Factory(coinTitle, coinCode, coinId)).get(RateChartPresenter::class.java)
        presenterView = presenter.view as RateChartView

        toolbar.title = coinTitle
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuFavorite -> {
                    presenter.onFavoriteClick()
                    true
                }
                R.id.menuUnfavorite -> {
                    presenter.onUnfavoriteClick()
                    true
                }
                R.id.menuNotification -> {
                    presenter.onNotificationClick()
                    true
                }
                else -> false
            }
        }
        notificationMenuItem = toolbar.menu.findItem(R.id.menuNotification)
        updateNotificationMenuItem()

        chart.setListener(this)
        chart.rateFormatter = presenter.rateFormatter

        observeData()
        bindActions()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        presenter.viewDidLoad()
    }

    private fun updateNotificationMenuItem() {
        notificationMenuItem?.apply {
            isVisible = presenter.notificationIconVisible
            icon = context?.let {
                val iconRes = if (presenter.notificationIconActive) R.drawable.ic_notification_24 else R.drawable.ic_notification_disabled
                ContextCompat.getDrawable(it, iconRes)
            }
        }
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
        presenterView.showSpinner.observe(viewLifecycleOwner, Observer {
            chart.showSinner()
        })

        presenterView.hideSpinner.observe(viewLifecycleOwner, Observer {
            chart.hideSinner()
        })

        presenterView.setDefaultMode.observe(viewLifecycleOwner, Observer { type ->
            actions[type]?.let { resetActions(it, setDefault = true) }
        })

        presenterView.showChartInfo.observe(viewLifecycleOwner, Observer { item ->
            chart.showChart()

            rootView.post {
                chart.setData(item.chartData, item.chartType)
            }

            emaChartIndicator.bind(item.emaTrend)
            macdChartIndicator.bind(item.macdTrend)
            rsiChartIndicator.bind(item.rsiTrend)

            coinRateDiff.diff = item.diffValue
        })

        presenterView.showMarketInfo.observe(viewLifecycleOwner, Observer { item ->
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

        presenterView.setSelectedPoint.observe(viewLifecycleOwner, Observer { item ->
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
                    getHistogramColor(it)?.let { it1 -> macdHistogram.setTextColor(it1) }
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

        presenterView.showError.observe(viewLifecycleOwner, Observer {
            chart.showError(getString(R.string.Charts_Error_NotAvailable))
        })

        presenterView.showEma.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showEma(enabled)
            emaChartIndicator.setStateEnabled(enabled)
        })

        presenterView.showMacd.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showMacd(enabled)
            macdChartIndicator.setStateEnabled(enabled)

            setViewVisibility(pointInfoVolume, pointInfoVolumeTitle, isVisible = !enabled)
            setViewVisibility(macdSignal, macdHistogram, macdValue, isVisible = enabled)
        })

        presenterView.showRsi.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showRsi(enabled)
            rsiChartIndicator.setStateEnabled(enabled)
        })

        presenterView.alertNotificationUpdated.observe(viewLifecycleOwner, Observer {
            updateNotificationMenuItem()
        })

        presenterView.showNotificationMenu.observe(viewLifecycleOwner, Observer { (coinId, coinName) ->
            BottomNotificationMenu.show(childFragmentManager, NotificationMenuMode.All, coinName, coinId)
        })

        presenterView.isFavorite.observe(viewLifecycleOwner, Observer {
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !it
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = it
        })

    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = App.numberFormatter.shortenValue(value)
        return formatter.formatFiat(shortCapValue.first, symbol, 0, 2) + " " + shortCapValue.second
    }

    private fun getHistogramColor(value: Float): Int? {
        val textColor = if (value > 0) R.color.green_d else R.color.red_d
        return context?.getColor(textColor)
    }

    private fun bindActions() {
        actions = mapOf(
                Pair(ChartType.TODAY, buttonToday),
                Pair(ChartType.DAILY, button24),
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

        val inLeftSide = chartActions.width / 2 < current.left
        if (setDefault) {
            chartActionsWrap.scrollTo(if (inLeftSide) chartActions.width else 0, 0)
            return
        }

        val by = if (inLeftSide) {
            chartActions.scrollX + current.width
        } else {
            chartActions.scrollX - current.width
        }

        chartActionsWrap.smoothScrollBy(by, 0)
    }

    private fun setViewVisibility(vararg views: View, isVisible: Boolean) {
        views.forEach { it.isInvisible = !isVisible }
    }

    companion object {
        private const val COIN_CODE_KEY = "coin_code_key"
        private const val COIN_TITLE_KEY = "coin_title_key"
        private const val COIN_ID_KEY = "coin_id_key"

        fun prepareParams(coinCode: String, coinTitle: String, coinId: String?) : Bundle {
            return bundleOf(
                    COIN_CODE_KEY to coinCode,
                    COIN_TITLE_KEY to coinTitle,
                    COIN_ID_KEY to coinId
            )
        }
    }
}
