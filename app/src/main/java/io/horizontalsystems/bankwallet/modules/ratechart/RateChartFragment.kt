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
import com.google.android.material.tabs.TabLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.bankwallet.ui.extensions.createTextView
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import kotlinx.android.synthetic.main.fragment_rate_chart.*
import java.math.BigDecimal
import java.util.*

class RateChartFragment : BaseFragment(), Chart.Listener, TabLayout.OnTabSelectedListener {
    private lateinit var presenter: RateChartPresenter
    private lateinit var presenterView: RateChartView

    private val formatter = App.numberFormatter
    private var notificationMenuItem: MenuItem? = null
    private val actions = listOf(
            Pair(ChartType.TODAY, R.string.Charts_TimeDuration_Today),
            Pair(ChartType.DAILY, R.string.Charts_TimeDuration_Day),
            Pair(ChartType.WEEKLY, R.string.Charts_TimeDuration_Week),
            Pair(ChartType.WEEKLY2, R.string.Charts_TimeDuration_TwoWeeks),
            Pair(ChartType.MONTHLY, R.string.Charts_TimeDuration_Month),
            Pair(ChartType.MONTHLY3, R.string.Charts_TimeDuration_Month3),
            Pair(ChartType.MONTHLY6, R.string.Charts_TimeDuration_HalfYear),
            Pair(ChartType.MONTHLY12, R.string.Charts_TimeDuration_Year),
            Pair(ChartType.MONTHLY24, R.string.Charts_TimeDuration_Year2)
    )

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
        setViewVisibility(tabLayout, isVisible = false)
    }

    override fun onTouchUp() {
        scroller.setScrollingEnabled(true)

        setViewVisibility(chartPointsInfo, isVisible = false)
        setViewVisibility(tabLayout, isVisible = true)
    }

    override fun onTouchSelect(point: PointInfo) {
        presenter.onTouchSelect(point)
    }

    //  TabLayout.OnTabSelectedListener

    override fun onTabSelected(tab: TabLayout.Tab) {
        presenter.onSelect(actions[tab.position].first)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
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
            val indexOf = actions.indexOfFirst { it.first == type }
            if (indexOf > -1) {
                tabLayout.removeOnTabSelectedListener(this)
                tabLayout.selectTab(tabLayout.getTabAt(indexOf))
                tabLayout.addOnTabSelectedListener(this)
            }
        })

        presenterView.showChartInfo.observe(viewLifecycleOwner, Observer { item ->
            chart.showChart()

            rootView.post {
                chart.setData(item.chartData, item.chartType)
            }

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
            indicatorEMA.isChecked = enabled
        })

        presenterView.showMacd.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showMacd(enabled)
            indicatorMACD.isChecked = enabled

            setViewVisibility(pointInfoVolume, pointInfoVolumeTitle, isVisible = !enabled)
            setViewVisibility(macdSignal, macdHistogram, macdValue, isVisible = enabled)
        })

        presenterView.showRsi.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showRsi(enabled)
            indicatorRSI.isChecked = enabled
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
        actions.forEach { (_, textId) ->
            tabLayout.newTab()
                    .setCustomView(createTextView(requireContext(), R.style.TabComponent).apply {
                        id = android.R.id.text1
                    })
                    .setText(getString(textId))
                    .let {
                        tabLayout.addTab(it, false)
                    }
        }

        tabLayout.tabRippleColor = null
        tabLayout.setSelectedTabIndicator(null)
        tabLayout.addOnTabSelectedListener(this)

        indicatorEMA.setOnClickListener {
            presenter.toggleEma()
        }

        indicatorMACD.setOnClickListener {
            presenter.toggleMacd()
        }

        indicatorRSI.setOnClickListener {
            presenter.toggleRsi()
        }
    }

    private fun setViewVisibility(vararg views: View, isVisible: Boolean) {
        views.forEach { it.isInvisible = !isVisible }
    }

    companion object {
        private const val COIN_CODE_KEY = "coin_code_key"
        private const val COIN_TITLE_KEY = "coin_title_key"
        private const val COIN_ID_KEY = "coin_id_key"

        fun prepareParams(coinCode: String, coinTitle: String, coinId: String?): Bundle {
            return bundleOf(
                    COIN_CODE_KEY to coinCode,
                    COIN_TITLE_KEY to coinTitle,
                    COIN_ID_KEY to coinId
            )
        }
    }
}
