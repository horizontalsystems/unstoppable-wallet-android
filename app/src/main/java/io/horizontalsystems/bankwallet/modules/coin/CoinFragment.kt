package io.horizontalsystems.bankwallet.modules.coin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.style.URLSpan
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
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.CoinInfoItemView
import io.horizontalsystems.bankwallet.ui.extensions.createTextView
import io.horizontalsystems.chartview.Chart
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.SettingsView
import io.horizontalsystems.xrateskit.entities.*
import kotlinx.android.synthetic.main.coin_market_details.*
import kotlinx.android.synthetic.main.fragment_coin.*
import java.math.BigDecimal
import java.util.*

class CoinFragment : BaseFragment(), Chart.Listener, TabLayout.OnTabSelectedListener {

    private val coinTitle by lazy {
        requireArguments().getString(COIN_TITLE_KEY) ?: ""
    }
    private val coinCode by lazy {
        requireArguments().getString(COIN_CODE_KEY) ?: ""
    }
    private val vmFactory by lazy {
        CoinModule.Factory(
                coinTitle,
                requireArguments().getParcelable(COIN_TYPE_KEY)!!,
                coinCode,
                requireArguments().getString(COIN_ID_KEY)
        )
    }

    private val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) { vmFactory }

    private val formatter = App.numberFormatter
    private var notificationMenuItem: MenuItem? = null
    private val actions = listOf(
            Pair(ChartType.TODAY, R.string.CoinPage_TimeDuration_Today),
            Pair(ChartType.DAILY, R.string.CoinPage_TimeDuration_Day),
            Pair(ChartType.WEEKLY, R.string.CoinPage_TimeDuration_Week),
            Pair(ChartType.WEEKLY2, R.string.CoinPage_TimeDuration_TwoWeeks),
            Pair(ChartType.MONTHLY, R.string.CoinPage_TimeDuration_Month),
            Pair(ChartType.MONTHLY3, R.string.CoinPage_TimeDuration_Month3),
            Pair(ChartType.MONTHLY6, R.string.CoinPage_TimeDuration_HalfYear),
            Pair(ChartType.MONTHLY12, R.string.CoinPage_TimeDuration_Year),
            Pair(ChartType.MONTHLY24, R.string.CoinPage_TimeDuration_Year2)
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = coinTitle
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuFavorite -> {
                    viewModel.onFavoriteClick()
                    true
                }
                R.id.menuUnfavorite -> {
                    viewModel.onUnfavoriteClick()
                    true
                }
                R.id.menuNotification -> {
                    viewModel.onNotificationClick()
                    true
                }
                else -> false
            }
        }
        notificationMenuItem = toolbar.menu.findItem(R.id.menuNotification)
        updateNotificationMenuItem()

        chart.setListener(this)
        chart.rateFormatter = viewModel.rateFormatter

        observeData()
        bindActions()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        aboutTextToggle.setOnClickListener {
            if (aboutText.maxLines == Integer.MAX_VALUE) {
                aboutText.maxLines = 8
                aboutTextToggle.text = "Read More"
            } else {
                aboutText.maxLines = Integer.MAX_VALUE
                aboutTextToggle.text = "Read Less"
            }
        }


    }

    private fun updateNotificationMenuItem() {
        notificationMenuItem?.apply {
            isVisible = viewModel.notificationIconVisible
            icon = context?.let {
                val iconRes = if (viewModel.notificationIconActive) R.drawable.ic_notification_24 else R.drawable.ic_notification_disabled
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
        viewModel.onTouchSelect(point)
    }

    //  TabLayout.OnTabSelectedListener

    override fun onTabSelected(tab: TabLayout.Tab) {
        viewModel.onSelect(actions[tab.position].first)
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
    }

    //  Private

    private fun observeData() {
        viewModel.chartSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            if (isLoading) {
                chart.showSinner()
            } else {
                chart.hideSinner()
            }
        })

        viewModel.marketSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            marketSpinner.isVisible = isLoading
        })

        viewModel.setDefaultMode.observe(viewLifecycleOwner, Observer { type ->
            val indexOf = actions.indexOfFirst { it.first == type }
            if (indexOf > -1) {
                tabLayout.removeOnTabSelectedListener(this)
                tabLayout.selectTab(tabLayout.getTabAt(indexOf))
                tabLayout.addOnTabSelectedListener(this)
            }
        })

        viewModel.showChartInfo.observe(viewLifecycleOwner, Observer { item ->
            chart.showChart()
            setViewVisibility(indicatorEMA, indicatorMACD, indicatorRSI, isVisible = true)

            rootView.post {
                chart.setData(item.chartData, item.chartType)
            }

            coinRateDiff.diff = item.diffValue
        })

        viewModel.coinDetailsLiveData.observe(viewLifecycleOwner, Observer { item ->
            marketDetails.isVisible = true

            coinRateLast.text = formatter.formatFiat(item.rateValue, item.currency.symbol, 2, 4)

            setMarketData(item.marketDataList)

            // Coin Markets

            coinMarketsButton.showTitle(getString(R.string.CoinPage_CoinMarket, coinCode))
            coinMarketsButton.setOnClickListener {
                findNavController().navigate(R.id.coinFragment_to_coinMarketsFragment, null, navOptions())
            }

            // Performance

            coinPerformanceView.bind(item.rateDiffs)

            // About

            aboutGroup.isVisible = item.coinMeta.description.isNotBlank()
            val aboutTextSpanned = Html.fromHtml(item.coinMeta.description.replace("\n", "<br />"), Html.FROM_HTML_MODE_COMPACT)
            aboutText.text = removeLinkSpans(aboutTextSpanned)

            // Categories/Platforms/Links
            setCategoriesAndPlatforms(item.coinMeta.categories, item.coinMeta.platforms)

            //Links
            setLinks(item.coinMeta.links)
        })

        viewModel.setSelectedPoint.observe(viewLifecycleOwner, Observer { item ->
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

        viewModel.showChartError.observe(viewLifecycleOwner, Observer {
            chart.showError(getString(R.string.CoinPage_Error_NotAvailable))
        })

        viewModel.showEma.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showEma(enabled)
            indicatorEMA.isChecked = enabled
        })

        viewModel.showMacd.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showMacd(enabled)
            indicatorMACD.isChecked = enabled

            setViewVisibility(pointInfoVolume, pointInfoVolumeTitle, isVisible = !enabled)
            setViewVisibility(macdSignal, macdHistogram, macdValue, isVisible = enabled)
        })

        viewModel.showRsi.observe(viewLifecycleOwner, Observer { enabled ->
            chart.showRsi(enabled)
            indicatorRSI.isChecked = enabled
        })

        viewModel.alertNotificationUpdated.observe(viewLifecycleOwner, Observer {
            updateNotificationMenuItem()
        })

        viewModel.showNotificationMenu.observe(viewLifecycleOwner, Observer { (coinId, coinName) ->
            BottomNotificationMenu.show(childFragmentManager, NotificationMenuMode.All, coinName, coinId)
        })

        viewModel.isFavorite.observe(viewLifecycleOwner, Observer {
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !it
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = it
        })

        viewModel.coinMarkets.observe(viewLifecycleOwner, { items ->
            coinMarketsButton.isVisible = items.isNotEmpty()
        })
    }

    private fun setMarketData(marketDataList: List<MarketData>) {
        marketDataLayout.removeAllViews()

        context?.let { context ->
            marketDataList.forEachIndexed { index, marketData ->
                val coinInfoView = CoinInfoItemView(context).apply {
                    bind(
                            title = getString(marketData.title),
                            value = marketData.value,
                            listPosition = ListPosition.getListPosition(marketDataList.size, index)
                    )
                }

                marketDataLayout.addView(coinInfoView)
            }
        }
    }

    private fun setLinks(links: Map<LinkType, String>) {
        context?.let { context ->
            linksLayout.removeAllViews()

            links.onEachIndexed { index, entry ->
                val link = SettingsView(context)

                when (entry.key) {
                    LinkType.GUIDE -> {
                        link.showTitle(getString(R.string.CoinPage_Guide))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_academy_20))
                    }
                    LinkType.WEBSITE -> {
                        link.showTitle(getString(R.string.CoinPage_Website))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_globe))
                    }
                    LinkType.WHITEPAPER -> {
                        link.showTitle(getString(R.string.CoinPage_Whitepaper))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_clipboard))
                    }
                    LinkType.TWITTER -> {
                        link.showTitle(getString(R.string.CoinPage_Twitter))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_twitter))
                    }
                    LinkType.TELEGRAM -> {
                        link.showTitle(getString(R.string.CoinPage_Telegram))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_telegram))
                    }
                    LinkType.REDDIT -> {
                        link.showTitle(getString(R.string.CoinPage_Reddit))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_reddit))
                    }
                    LinkType.GITHUB -> {
                        link.showTitle(getString(R.string.CoinPage_Github))
                        link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_github))
                    }
                }

                link.setListPosition(ListPosition.getListPosition(links.size, index))
                link.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.value)));
                }

                linksLayout.addView(link)
            }
        }
    }

    private fun setCategoriesAndPlatforms(categories: List<CoinCategory>, platforms: Map<CoinPlatformType, String>) {
        context?.let { context ->
            categoriesLayout.removeAllViews()
            val categoryCellsCount = if (categories.isNotEmpty()) 1 else 0
            val categoryPlatformCellsCount = platforms.size + categoryCellsCount

            if (categories.isNotEmpty()) {
                val categoriesView = CoinInfoItemView(context).apply {
                    bind(
                            title = getString(R.string.CoinPage_Category),
                            value = categories.joinToString(", ") { it.name },
                            listPosition = ListPosition.getListPosition(categoryPlatformCellsCount, 0)
                    )
                }

                categoriesLayout.addView(categoriesView)
            }

            platforms.onEachIndexed { index, (platform, value) ->
                val platformView = CoinInfoItemView(context).apply {
                    bind(
                            title = platform.name,
                            decoratedValue = value,
                            listPosition = ListPosition.getListPosition(categoryPlatformCellsCount, index + categoryCellsCount)
                    )
                }

                categoriesLayout.addView(platformView)
            }
        }
    }

    private fun removeLinkSpans(spanned: Spanned): Spannable {
        val spannable = SpannableString(spanned)
        for (u in spannable.getSpans(0, spannable.length, URLSpan::class.java)) {
            spannable.removeSpan(u)
        }
        return spannable
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
            viewModel.toggleEma()
        }

        indicatorMACD.setOnClickListener {
            viewModel.toggleMacd()
        }

        indicatorRSI.setOnClickListener {
            viewModel.toggleRsi()
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
