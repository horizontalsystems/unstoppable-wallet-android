package io.horizontalsystems.bankwallet.modules.ratechart

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
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
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoItemView
import io.horizontalsystems.bankwallet.ui.extensions.createTextView
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.SettingsView
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.LinkType
import io.horizontalsystems.xrateskit.entities.TimePeriod
import kotlinx.android.synthetic.main.coin_market.*
import kotlinx.android.synthetic.main.coin_market_details.*
import kotlinx.android.synthetic.main.coin_performance.*
import kotlinx.android.synthetic.main.coin_price_change.*
import kotlinx.android.synthetic.main.fragment_rate_chart.*
import kotlinx.android.synthetic.main.view_transaction_info_item.view.*
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

        val coinType = arguments?.getParcelable<CoinType>(COIN_TYPE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        val coinCode = arguments?.getString(COIN_CODE_KEY) ?: run {
            findNavController().popBackStack()
            return
        }

        val coinTitle = arguments?.getString(COIN_TITLE_KEY) ?: ""

        presenter = ViewModelProvider(this, RateChartModule.Factory(coinTitle, coinType, coinCode, coinId)).get(RateChartPresenter::class.java)
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
        presenterView.chartSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                chart.showSinner()
            } else {
                chart.hideSinner()
            }
        })

        presenterView.marketSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            marketSpinner.isVisible = isLoading
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
            setViewVisibility(indicatorEMA, indicatorMACD, indicatorRSI, isVisible = true)

            rootView.post {
                chart.setData(item.chartData, item.chartType)
            }

            coinRateDiff.diff = item.diffValue
        })

        presenterView.showMarketInfo.observe(viewLifecycleOwner, Observer { item ->
            marketDetails.isVisible = true

            coinRateLast.text = formatter.formatFiat(item.rateValue, item.currency.symbol, 2, 4)

            marketCapValue.apply {
                if (item.marketCap > BigDecimal.ZERO) {
                    text = formatFiatShortened(item.marketCap, item.currency.symbol)
                } else {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            volume24Value.apply {
                text = formatFiatShortened(item.volume24h, item.currency.symbol)
            }

            circulationValue.apply {
                if (item.circulatingSupply.value > BigDecimal.ZERO) {
                    text = formatter.formatCoin(item.circulatingSupply.value, item.circulatingSupply.coinCode, 0, 2)
                } else {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            totalSupplyValue.apply {
                if (item.totalSupply.value > BigDecimal.ZERO) {
                    text = formatter.formatCoin(item.totalSupply.value, item.totalSupply.coinCode, 0, 2)
                } else {
                    isEnabled = false
                    text = getString(R.string.NotAvailable)
                }
            }

            // Price
            priceRangeMin.text = formatter.formatFiat(item.rateLow24h, item.currency.symbol, 2, 4)
            priceRangeMax.text = formatter.formatFiat(item.rateHigh24h, item.currency.symbol, 2, 4)

            moveMarker(marker24h, item.rateValue.toFloat(), item.rateLow24h.toFloat(), item.rateHigh24h.toFloat())

            // Market

            marketCapPercent.text = formatter.format(item.marketCapDiff24h.abs(), 0, 2, suffix = "%")

            // Performance

            item.rateDiffs.forEach { (period, values) ->
                val usdValue = values["USD"] ?: BigDecimal.ZERO
                val ethValue = values["ETH"] ?: BigDecimal.ZERO
                val btcValue = values["BTC"] ?: BigDecimal.ZERO

                when (period) {
                    TimePeriod.DAY_7 -> {
                        setColoredPercentageValue(usd1WPercent, usdValue)
                        setColoredPercentageValue(eth1WPercent, ethValue)
                        setColoredPercentageValue(btc1WPercent, btcValue)
                    }
                    TimePeriod.DAY_30 -> {
                        setColoredPercentageValue(usd1MPercent, usdValue)
                        setColoredPercentageValue(eth1MPercent, ethValue)
                        setColoredPercentageValue(btc1MPercent, btcValue)
                    }
                }
            }

            // About

            aboutText.text = Html.fromHtml(item.coinMeta.description.replace("\n", "<br />"), Html.FROM_HTML_MODE_COMPACT)

            // Categories/Links

            context?.let { context ->
                linksLayout.removeAllViews()

                item.coinMeta.categories?.let { categories ->
                    val category = TransactionInfoItemView(context).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutHelper.dp(48f, context))

                        txViewBackground.setBackgroundColor(Color.TRANSPARENT)
                        txtTitle.text = getString(R.string.Charts_Category)
                        valueText.text = categories.map { it.name }.joinToString(", ")
                        valueText.isVisible = true
                        btnAction.isVisible = false
                        decoratedText.isVisible = false
                    }

                    linksLayout.addView(category)
                }

                item.coinMeta.platforms?.forEach { (platform, value) ->
                    val contract = TransactionInfoItemView(context).apply {
                        txViewBackground.setBackgroundColor(Color.TRANSPARENT)
                        bindHashId(platform.name, value)
                    }

                    linksLayout.addView(contract)
                }

                item.coinMeta.links[LinkType.GUIDE]?.let {
                    val link = SettingsView(context).apply {
                        showTitle(context.getString(R.string.Charts_Guide))
                        showIcon(ContextCompat.getDrawable(context, R.drawable.ic_academy_20))

                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = LayoutHelper.dp(32f, context)
                            bottomMargin = LayoutHelper.dp(32f, context)
                        }
                    }

                    linksLayout.addView(link)
                }

                val allLinks = item.coinMeta.links.filter { it.key != LinkType.GUIDE }

                allLinks.onEachIndexed { index, entry ->
                    val link = SettingsView(context)

                    when (entry.key) {
                        LinkType.GUIDE -> {
                            link.showTitle(getString(R.string.Charts_Guide))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_academy_20))
                        }
                        LinkType.WEBSITE -> {
                            link.showTitle(getString(R.string.Charts_Website))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_globe))
                        }
                        LinkType.WHITEPAPER -> {
                            link.showTitle(getString(R.string.Charts_Whitepaper))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_clipboard))
                        }
                        LinkType.TWITTER -> {
                            link.showTitle(getString(R.string.Charts_Twitter))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_twitter))
                        }
                        LinkType.TELEGRAM -> {
                            link.showTitle(getString(R.string.Charts_Telegram))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_telegram))
                        }
                        LinkType.REDDIT -> {
                            link.showTitle(getString(R.string.Charts_Reddit))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_reddit))
                        }
                        LinkType.GITHUB -> {
                            link.showTitle(getString(R.string.Charts_Github))
                            link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_github))
                        }
                    }

                    if (index == 0) {
                        link.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                            topMargin = LayoutHelper.dp(32f, context)
                        }
                    }

                    link.setListPosition(ListPosition.Companion.getListPosition(allLinks.size, index))
                    link.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.value)));
                    }

                    linksLayout.addView(link)
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

    private fun setColoredPercentageValue(textView: TextView, percentage: BigDecimal) {
        val color = if (percentage >= BigDecimal.ZERO) R.color.remus else R.color.lucian
        val sign = if (percentage >= BigDecimal.ZERO) "+" else "-"

        textView.setTextColor(requireContext().getColor(color))
        textView.text = formatter.format(percentage.abs(), 0, 2, prefix = sign, suffix = "%")
    }

    private fun moveMarker(markerView: ImageView, price: Float, low: Float, high: Float) {
        val priceMax = Math.max(high - low, 1f)
        val priceActual = Math.max(price - low, 1f)

        val markerWrapWidth = LayoutHelper.dp(32 * 2f, context)
        val percent = Math.min(100 * priceActual / priceMax, 98f)
        val coordinate = percent / 100 * (chart.width - markerWrapWidth)

        markerView.animate().translationX(coordinate)
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
        private const val COIN_TYPE_KEY = "coin_type_key"
        private const val COIN_CODE_KEY = "coin_code_key"
        private const val COIN_TITLE_KEY = "coin_title_key"
        private const val COIN_ID_KEY = "coin_id_key"

        fun prepareParams(coinType: CoinType, coinCode: String, coinTitle: String, coinId: String?): Bundle {
            return bundleOf(
                    COIN_TYPE_KEY to coinType,
                    COIN_CODE_KEY to coinCode,
                    COIN_TITLE_KEY to coinTitle,
                    COIN_ID_KEY to coinId
            )
        }
    }
}
