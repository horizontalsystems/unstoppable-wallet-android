package io.horizontalsystems.bankwallet.modules.coin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.*
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
import io.horizontalsystems.xrateskit.entities.LinkType
import kotlinx.android.synthetic.main.coin_market_details.*
import kotlinx.android.synthetic.main.fragment_coin.*

class CoinFragment : BaseFragment(), CoinChartAdapter.Listener, CoinDataAdapter.Listener {

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
        val coinRoiAdapter = CoinRoiAdapter(viewModel.roiLiveData, viewLifecycleOwner)
        val marketDataAdapter = CoinDataAdapter(viewModel.marketDataLiveData, viewLifecycleOwner, this)
        val tvlDataAdapter = CoinDataAdapter(viewModel.tvlDataLiveData, viewLifecycleOwner, this)
        val categoriesAdapter = CoinCategoryAdapter(viewModel.categoriesLiveData, viewLifecycleOwner)
        val contractInfoAdapter = CoinDataAdapter(viewModel.contractInfoLiveData, viewLifecycleOwner, this)
        val aboutAdapter = CoinAboutAdapter(viewModel.aboutTextLiveData, viewLifecycleOwner)

        val concatAdapter = ConcatAdapter(
                subtitleAdapter,
                chartAdapter,
                SpacerAdapter(),
                coinRoiAdapter,
                SpacerAdapter(),
                marketDataAdapter,
                SpacerAdapter(),
                tvlDataAdapter,
                categoriesAdapter,
                SpacerAdapter(),
                contractInfoAdapter,
                aboutAdapter
        )

        controlledRecyclerView.adapter = concatAdapter

        observeData()

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
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

    //CoinDataAdapter.Listener

    override fun onClick(clickType: CoinDataClickType) {
        when(clickType){
            CoinDataClickType.MetricChart -> MetricChartFragment.show(childFragmentManager, MetricChartType.Coin(viewModel.coinType))
            is CoinDataClickType.Link -> {
                //open link
            }
        }
    }

    //  Private

    private fun observeData() {
        viewModel.marketSpinner.observe(viewLifecycleOwner, Observer { isLoading ->
            marketSpinner.isVisible = isLoading
        })

        viewModel.coinDetailsLiveData.observe(viewLifecycleOwner, Observer { item ->
            marketDetails.isVisible = true

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

    companion object {
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
