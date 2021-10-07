package io.horizontalsystems.bankwallet.modules.coin.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinDataClickType
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.CoinViewModel
import io.horizontalsystems.bankwallet.modules.coin.PoweredByAdapter
import io.horizontalsystems.bankwallet.modules.coin.adapters.*
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter.ChartViewType
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartType
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.xrateskit.entities.LinkType
import kotlinx.android.synthetic.main.fragment_coin_overview.*

class CoinOverviewFragment : BaseFragment(), CoinChartAdapter.Listener, CoinDataAdapter.Listener, CoinLinksAdapter.Listener {

    private val vmFactory by lazy { CoinOverviewModule.Factory(coinViewModel.fullCoin) }

    private val coinViewModel by navGraphViewModels<CoinViewModel>(R.id.coinFragment)
    private val viewModel by navGraphViewModels<CoinOverviewViewModel>(R.id.coinFragment) { vmFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_coin_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val subtitleAdapter = CoinSubtitleAdapter(viewModel.subtitleLiveData, viewLifecycleOwner)
        val chartAdapter = CoinChartAdapter(
            viewModel.chartInfoLiveData,
            viewModel.currency,
            ChartViewType.CoinChart,
            this,
            viewLifecycleOwner
        )
        val coinRoiAdapter = CoinRoiAdapter(viewModel.roiLiveData, viewLifecycleOwner)
        val marketDataAdapter = CoinDataAdapter(viewModel.marketDataLiveData, viewLifecycleOwner, this)
        val tradingVolumeAdapter = CoinDataAdapter(viewModel.tradingVolumeLiveData, viewLifecycleOwner, this)
        val tvlDataAdapter = CoinDataAdapter(viewModel.tvlDataLiveData, viewLifecycleOwner, this)
        val investorDataAdapter = CoinDataAdapter(viewModel.investorDataLiveData, viewLifecycleOwner, this, R.string.CoinPage_InvestorData)
        val securityParamsAdapter = CoinDataAdapter(viewModel.securityParamsLiveData, viewLifecycleOwner, this, R.string.CoinPage_SecurityParams)
        val categoriesAdapter = CoinCategoryAdapter(viewModel.categoriesLiveData, viewLifecycleOwner)
        val contractInfoAdapter = CoinDataAdapter(viewModel.contractInfoLiveData, viewLifecycleOwner, this)
        val aboutAdapter = CoinAboutAdapter(viewModel.aboutTextLiveData, viewLifecycleOwner)
        val linksAdapter = CoinLinksAdapter(viewModel.linksLiveData, viewLifecycleOwner, this)
        val footerAdapter = PoweredByAdapter(viewModel.showFooterLiveData, viewLifecycleOwner, getString(R.string.Market_PoweredByApi))

        val loadingAdapter = CoinLoadingAdapter(viewModel.loadingLiveData, viewLifecycleOwner)
        val errorAdapter = CoinInfoErrorAdapter(viewModel.coinInfoErrorLiveData, viewLifecycleOwner)

        val concatAdapter = ConcatAdapter(
                subtitleAdapter,
                chartAdapter,
                marketDataAdapter,
                coinRoiAdapter,
                tradingVolumeAdapter,
                tvlDataAdapter,
                investorDataAdapter,
                securityParamsAdapter,
                categoriesAdapter,
                contractInfoAdapter,
                aboutAdapter,
                linksAdapter,
                loadingAdapter,
                errorAdapter,
                footerAdapter
        )

        controlledRecyclerView.adapter = concatAdapter

        activity?.onBackPressedDispatcher?.addCallback(this) {
            findNavController().popBackStack()
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
                context?.let { ctx ->
                    LinkHelper.openLinkInAppBrowser(ctx, coinLink.url.trim())
                }
            }
        }
    }

    //  CoinDataAdapter.Listener

    override fun onClick(clickType: CoinDataClickType) {
        when (clickType){
            CoinDataClickType.MetricChart -> MetricChartFragment.show(childFragmentManager, MetricChartType.Coin(viewModel.coinType))
            CoinDataClickType.TradingVolumeMetricChart -> MetricChartFragment.show(childFragmentManager, MetricChartType.TradingVolume(viewModel.coinType))
            CoinDataClickType.Markets -> findNavController().navigate(R.id.coinFragment_to_coinMarketsFragment, null, navOptions())
            CoinDataClickType.TvlRank -> findNavController().navigate(R.id.coinFragment_to_tvlRankFragment, null, navOptions())
            CoinDataClickType.FundsInvested -> findNavController().navigate(R.id.coinFragment_to_coinInvestorsFragment, null, navOptions())
            CoinDataClickType.MajorHolders -> findNavController().navigate(R.id.coinFragment_to_coinMajorHoldersFragment, null, navOptions())
            is CoinDataClickType.SecurityAudits -> {
                findNavController().navigate(R.id.coinFragment_to_coinAuditsFragment, bundleOf("coinType" to clickType.coinType), navOptions())
            }
            is CoinDataClickType.SecurityInfo -> {
                findNavController().navigate(R.id.coinFragment_to_coinSecurityInfoFragment, bundleOf("info" to clickType), navOptions())
            }
        }
    }

}
