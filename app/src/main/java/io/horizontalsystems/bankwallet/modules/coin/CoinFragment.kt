package io.horizontalsystems.bankwallet.modules.coin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinSubtitleAdapter
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartType
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.CoinInfoItemView
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.SettingsView
import io.horizontalsystems.views.helpers.LayoutHelper
import io.horizontalsystems.xrateskit.entities.CoinCategory
import io.horizontalsystems.xrateskit.entities.CoinMeta
import io.horizontalsystems.xrateskit.entities.LinkType
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.spans.LastLineSpacingSpan
import kotlinx.android.synthetic.main.coin_market_details.*
import kotlinx.android.synthetic.main.fragment_coin.*
import org.commonmark.node.Heading
import org.commonmark.node.Paragraph

class CoinFragment : BaseFragment(), CoinChartAdapter.Listener {

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
                coinCode
        )
    }

    private val viewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment) { vmFactory }

    private val formatter = App.numberFormatter
    private var notificationMenuItem: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.title = coinCode
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

        val subtitleAdapter = CoinSubtitleAdapter(viewModel.subtitleLiveData, viewLifecycleOwner)
        val chartAdapter = CoinChartAdapter(viewModel, viewLifecycleOwner, this)

        val concatAdapter = ConcatAdapter(subtitleAdapter, chartAdapter)

        controlledRecyclerView.adapter = concatAdapter

        observeData()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
        }

        aboutTextToggle.setOnClickListener {
            if (aboutText.maxLines == Integer.MAX_VALUE) {
                aboutText.maxLines = ABOUT_MAX_LINES
                aboutTextToggle.text = getString(R.string.CoinPage_ReadMore)
            } else {
                aboutText.maxLines = Integer.MAX_VALUE
                aboutTextToggle.text = getString(R.string.CoinPage_ReadLess)
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

    //  CoinChartAdapter Listener

    override fun onChartTouchDown() {
        controlledRecyclerView.enableVerticalScroll(false)
    }

    override fun onChartTouchUp() {
        controlledRecyclerView.enableVerticalScroll(true)
    }

    override fun onTabSelect(chartType: ChartView.ChartType) {
        viewModel.onSelect(chartType)
    }

    //  Private

    private fun observeData() {
        viewModel.marketSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            marketSpinner.isVisible = isLoading
        })

        viewModel.coinDetailsLiveData.observe(viewLifecycleOwner, Observer { item ->
            marketDetails.isVisible = true

            // Performance

            setCoinPerformance(item)

            // Market

            setMarketData(item.marketDataList)

            // TVL

            setTvlData(item.tvlInfo)


            // About

            if (item.coinMeta.description.isNotBlank()) {
                aboutGroup.isVisible = true
                aboutTitle.isVisible = item.coinMeta.descriptionType == CoinMeta.DescriptionType.HTML
                val aboutTextSpanned = when (item.coinMeta.descriptionType) {
                    CoinMeta.DescriptionType.HTML -> {
                        Html.fromHtml(item.coinMeta.description.replace("\n", "<br />"), Html.FROM_HTML_MODE_COMPACT)
                    }
                    CoinMeta.DescriptionType.MARKDOWN -> {
                        val markwon = Markwon.builder(requireContext())
                                .usePlugin(object : AbstractMarkwonPlugin() {

                                    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                                        builder.setFactory(Heading::class.java) { configuration, props ->
                                            arrayOf(
                                                    TextAppearanceSpan(context, R.style.Headline2),
                                                    ForegroundColorSpan(resources.getColor(R.color.bran, null))
                                            )
                                        }
                                        builder.setFactory(Paragraph::class.java) { configuration, props ->
                                            arrayOf(
                                                    LastLineSpacingSpan(LayoutHelper.dp(24f, requireContext())),
                                                    TextAppearanceSpan(context, R.style.Subhead2),
                                                    ForegroundColorSpan(resources.getColor(R.color.grey, null))
                                            )
                                        }
                                    }
                                })
                                .build()

                        markwon.toMarkdown(item.coinMeta.description)
                    }
                }

                aboutText.text = removeLinkSpans(aboutTextSpanned)
                aboutText.maxLines = Integer.MAX_VALUE
                aboutText.isVisible = false
                aboutText.doOnPreDraw {
                    if (aboutText.lineCount > ABOUT_MAX_LINES + ABOUT_TOGGLE_LINES) {
                        aboutText.maxLines = ABOUT_MAX_LINES
                        aboutTextToggle.isVisible = true
                    } else {
                        aboutTextToggle.isVisible = false
                    }
                }
                aboutText.isVisible = true
            } else {
                aboutGroup.isVisible = false
            }
            // Categories/Platforms/Links
            setCategoriesAndContractInfo(item.coinMeta.categories, item.contractInfo)

            //Links
            setLinks(item.coinMeta.links, item.guideUrl)
        })

        viewModel.alertNotificationUpdated.observe(viewLifecycleOwner, Observer {
            updateNotificationMenuItem()
        })

        viewModel.showNotificationMenu.observe(viewLifecycleOwner, Observer { (coinType, coinName) ->
            BottomNotificationMenu.show(childFragmentManager, NotificationMenuMode.All, coinName, coinType)
        })

        viewModel.isFavorite.observe(viewLifecycleOwner, Observer {
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !it
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = it
        })

        viewModel.extraPages.observe(viewLifecycleOwner, { pages ->
            setExtraPages(pages)
        })
    }

    private fun setCoinPerformance(item: CoinDetailsViewItem) {
        if (item.rateDiffs.isEmpty()) {
            return
        }
        context?.let { ctx ->
            item.rateDiffs.forEachIndexed { index, rowViewItem ->
                val row = when (rowViewItem) {
                    is RoiViewItem.HeaderRowViewItem -> {
                        CoinPerformanceRowView(ctx).apply {
                            bindHeader(rowViewItem.title, rowViewItem.periods)
                        }
                    }
                    is RoiViewItem.RowViewItem -> {
                        CoinPerformanceRowView(ctx).apply {
                            bind(rowViewItem.title, rowViewItem.values, item.rateDiffs.size - 1, index)
                        }
                    }
                }
                coinPerformanceLayout.addView(row)
            }

        }
    }

    private fun setExtraPages(pages: List<CoinExtraPage>) {
        extraPagesLayout.removeAllViews()

        context?.let { context ->
            pages.forEach { item ->
                val coinInfoView = CoinInfoItemView(context).apply {
                    when (item) {
                        is CoinExtraPage.TradingVolume -> {
                            bind(
                                    title = getString(R.string.CoinPage_TradingVolume),
                                    value = item.value,
                                    listPosition = item.position,
                                    icon = if (item.canShowMarkets) R.drawable.ic_arrow_right else null
                            )
                            if (item.canShowMarkets) {
                                setOnClickListener {
                                    findNavController().navigate(R.id.coinFragment_to_coinMarketsFragment, null, navOptions())
                                }
                            }
                        }
                        is CoinExtraPage.Investors -> {
                            bind(
                                    title = getString(R.string.CoinPage_FundsInvested),
                                    listPosition = item.position,
                                    icon = R.drawable.ic_arrow_right
                            )
                            setOnClickListener {
                                findNavController().navigate(R.id.coinFragment_to_coinInvestorsFragment, null, navOptions())
                            }
                        }
                    }
                }

                extraPagesLayout.addView(coinInfoView)
            }
        }
    }

    private fun setMarketData(marketDataList: List<CoinDataItem>) {
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

    private fun setTvlData(tvlDataList: List<CoinDataItem>) {
        tvlLayout.removeAllViews()

        context?.let { context ->
            tvlDataList.forEachIndexed { index, marketData ->
                val view = CoinInfoItemView(context).apply {
                    bind(
                            title = getString(marketData.title),
                            value = marketData.value,
                            listPosition = ListPosition.getListPosition(tvlDataList.size, index),
                            icon = marketData.icon
                    )
                }

                if (index == 0) {
                    view.setOnClickListener {
                        MetricChartFragment.show(childFragmentManager, MetricChartType.Coin(viewModel.coinType))
                    }
                }

                tvlLayout.addView(view)
            }
        }
    }

    private fun setLinks(links: Map<LinkType, String>, guideUrl: String?) {
        context?.let { context ->
            linksLayout.removeAllViews()

            guideUrl?.let {
                val link = SettingsView(context)
                link.showTitle(getString(R.string.CoinPage_Guide))
                link.showIcon(ContextCompat.getDrawable(context, R.drawable.ic_academy_20))
                link.setListPosition(ListPosition.getListPosition(links.size + 1, 0))
                link.setOnClickListener {
                    val arguments = bundleOf(
                            MarkdownFragment.markdownUrlKey to guideUrl,
                            MarkdownFragment.handleRelativeUrlKey to true
                    )
                    findNavController().navigate(R.id.coinFragment_to_markdownFragment, arguments, navOptions())
                }
                linksLayout.addView(link)
            }

            links.onEachIndexed { index, entry ->
                val link = SettingsView(context)
                when (entry.key) {
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
                val shiftPosition = if (guideUrl != null) 1 else 0
                link.setListPosition(ListPosition.getListPosition(links.size + shiftPosition, index + shiftPosition))
                link.setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.value.trim())))
                }

                linksLayout.addView(link)
            }
        }
    }

    private fun setCategoriesAndContractInfo(categories: List<CoinCategory>, contractInfo: ContractInfo?) {
        categoriesText.text = categories.joinToString(", ") { it.name }
        categoriesGroup.isVisible = categories.isNotEmpty()

        context?.let { context ->
            platformsLayout.removeAllViews()

            contractInfo?.let {
                val platformView = CoinInfoItemView(context).apply {
                    bind(
                            title = contractInfo.title,
                            decoratedValue = contractInfo.value,
                            listPosition = ListPosition.Single
                    )
                }

                platformsLayout.addView(platformView)
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

    companion object {
        private const val ABOUT_MAX_LINES = 8
        private const val ABOUT_TOGGLE_LINES = 2
        private const val COIN_TYPE_KEY = "coin_type_key"
        private const val COIN_CODE_KEY = "coin_code_key"
        private const val COIN_TITLE_KEY = "coin_title_key"

        fun prepareParams(coinType: CoinType, coinCode: String, coinTitle: String): Bundle {
            return bundleOf(
                    COIN_TYPE_KEY to coinType,
                    COIN_CODE_KEY to coinCode,
                    COIN_TITLE_KEY to coinTitle
            )
        }
    }
}
