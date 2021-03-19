package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.MarketLoadingAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewModel
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_overview.*

class MarketOverviewFragment : BaseFragment(), ViewHolderMarketOverviewItem.Listener {

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketOverviewViewModel by viewModels<MarketOverviewViewModel> { MarketOverviewModule.Factory() }
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)

        val topLoadingAdapter = MarketLoadingAdapter(marketOverviewViewModel.loadingLiveData, marketOverviewViewModel.errorLiveData, marketOverviewViewModel::onErrorClick, viewLifecycleOwner)
        val topGainersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopGainers, R.drawable.ic_circle_up_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopGainers)
                }
        )

        val topGainersAdapter = MarketOverviewItemsAdapter(
                this,
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner)

        val topLosersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopLosers, R.drawable.ic_circle_down_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopLosers)
                }
        )

        val topLosersAdapter = MarketOverviewItemsAdapter(
                this,
                marketOverviewViewModel.topLosersViewItemsLiveData,
                viewLifecycleOwner)

        val topByVolumeHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner,
                MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopByVolume, R.drawable.ic_chart_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopByVolume)
                }
        )
        val topByVolumeAdapter = MarketOverviewItemsAdapter(
                this,
                marketOverviewViewModel.topByVolumeViewItemsLiveData,
                viewLifecycleOwner)

        val poweredByAdapter = PoweredByAdapter(marketOverviewViewModel.showPoweredByLiveData, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(
                marketMetricsAdapter,
                topLoadingAdapter,
                topGainersHeaderAdapter,
                topGainersAdapter,
                topLosersHeaderAdapter,
                topLosersAdapter,
                topByVolumeHeaderAdapter,
                topByVolumeAdapter,
                poweredByAdapter
        )
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketOverviewViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketMetricsViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }

        marketOverviewViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireView(), it)
        }
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinType, marketViewItem.coinCode, marketViewItem.coinName)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }
}
