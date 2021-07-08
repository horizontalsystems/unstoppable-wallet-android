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
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.adapters.*
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.market.overview.PoweredByAdapter
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartType
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.BottomNotificationMenu
import io.horizontalsystems.bankwallet.modules.settings.notifications.bottommenu.NotificationMenuMode
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.xrateskit.entities.LinkType
import kotlinx.android.synthetic.main.fragment_coin.*

class CoinFragment : BaseFragment(), CoinChartAdapter.Listener, CoinDataAdapter.Listener, CoinLinksAdapter.Listener {

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
                    HudHelper.showSuccessMessage(requireView(), getString(R.string.Hud_Added_To_Watchlist))
                    true
                }
                R.id.menuUnfavorite -> {
                    viewModel.onUnfavoriteClick()
                    HudHelper.showSuccessMessage(requireView(), getString(R.string.Hud_Removed_from_Watchlist))
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
        val tradingVolumeAdapter = CoinDataAdapter(viewModel.tradingVolumeLiveData, viewLifecycleOwner, this)
        val tvlDataAdapter = CoinDataAdapter(viewModel.tvlDataLiveData, viewLifecycleOwner, this)
        val investorDataAdapter = CoinDataAdapter(viewModel.investorDataLiveData, viewLifecycleOwner, this, R.string.CoinPage_InvestorData)
        val categoriesAdapter = CoinCategoryAdapter(viewModel.categoriesLiveData, viewLifecycleOwner)
        val contractInfoAdapter = CoinDataAdapter(viewModel.contractInfoLiveData, viewLifecycleOwner, this)
        val aboutAdapter = CoinAboutAdapter(viewModel.aboutTextLiveData, viewLifecycleOwner)
        val linksAdapter = CoinLinksAdapter(viewModel.linksLiveData, viewLifecycleOwner, this)
        val footerAdapter = PoweredByAdapter(viewModel.showFooterLiveData, viewLifecycleOwner)

        val loadingAdapter = CoinLoadingAdapter(viewModel.loadingLiveData, viewLifecycleOwner)
        val errorAdapter = CoinInfoErrorAdapter(viewModel.coinInfoErrorLiveData, viewLifecycleOwner)

        val concatAdapter = ConcatAdapter(
                subtitleAdapter,
                chartAdapter,
                coinRoiAdapter,
                marketDataAdapter,
                tradingVolumeAdapter,
                tvlDataAdapter,
                investorDataAdapter,
                categoriesAdapter,
                contractInfoAdapter,
                aboutAdapter,
                linksAdapter,
                loadingAdapter,
                errorAdapter,
                footerAdapter
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

    //  CoinLinksAdapter Listener

    override fun onClick(coinLink: CoinLink) {
        when(coinLink.linkType){
            LinkType.GUIDE -> {
                val arguments = bundleOf(
                        MarkdownFragment.markdownUrlKey to coinLink.url,
                        MarkdownFragment.handleRelativeUrlKey to true
                )
                findNavController().navigate(R.id.coinFragment_to_markdownFragment, arguments, navOptions())
            }
            else -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(coinLink.url.trim())))
            }
        }
    }

    //CoinDataAdapter.Listener

    override fun onClick(clickType: CoinDataClickType) {
        when(clickType){
            CoinDataClickType.MetricChart -> MetricChartFragment.show(childFragmentManager, MetricChartType.Coin(viewModel.coinType))
            CoinDataClickType.TradingVolumeMetricChart -> MetricChartFragment.show(childFragmentManager, MetricChartType.TradingVolume(viewModel.coinType))
            CoinDataClickType.TradingVolume -> findNavController().navigate(R.id.coinFragment_to_coinMarketsFragment, null, navOptions())
            CoinDataClickType.TvlRank -> findNavController().navigate(R.id.coinFragment_to_tvlRankFragment, null, navOptions())
            CoinDataClickType.FundsInvested -> findNavController().navigate(R.id.coinFragment_to_coinInvestorsFragment, null, navOptions())
            CoinDataClickType.MajorHolders -> findNavController().navigate(R.id.coinFragment_to_coinMajorHoldersFragment, null, navOptions())
        }
    }

    //  Private

    private fun observeData() {
        viewModel.alertNotificationUpdated.observe(viewLifecycleOwner, Observer {
            updateNotificationMenuItem()
        })

        viewModel.showNotificationMenu.observe(viewLifecycleOwner, Observer { (coinType, coinName) ->
            BottomNotificationMenu.show(childFragmentManager, NotificationMenuMode.All, coinName, coinType)
        })

        viewModel.isFavorite.observe(viewLifecycleOwner, Observer { isFavorite ->
            toolbar.menu.findItem(R.id.menuFavorite).isVisible = !isFavorite
            toolbar.menu.findItem(R.id.menuUnfavorite).isVisible = isFavorite
        })

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
