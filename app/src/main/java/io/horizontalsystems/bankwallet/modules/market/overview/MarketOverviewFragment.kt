package io.horizontalsystems.bankwallet.modules.market.overview

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.viewModels
import androidx.lifecycle.Transformations
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketLoadingAdapter
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewModel
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.market.posts.MarketPostItemsAdapter
import io.horizontalsystems.bankwallet.modules.market.posts.ViewHolderMarketPostItem
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFragment
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartType
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_overview.*

class MarketOverviewFragment : BaseFragment(), ViewHolderMarketOverviewItem.Listener, ViewHolderMarketPostItem.Listener {

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
                hasItemsLiveData = Transformations.map(marketOverviewViewModel.topGainersViewItemsLiveData) { it.isNotEmpty() },
                viewLifecycleOwner = viewLifecycleOwner,
                settingsHeaderItem = MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopGainers, R.drawable.ic_circle_up_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopGainers)
                }
        )

        val topGainersAdapter = MarketOverviewItemsAdapter(
                this,
                marketOverviewViewModel.topGainersViewItemsLiveData,
                viewLifecycleOwner)

        val topLosersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                hasItemsLiveData = Transformations.map(marketOverviewViewModel.topLosersViewItemsLiveData) { it.isNotEmpty() },
                viewLifecycleOwner = viewLifecycleOwner,
                settingsHeaderItem = MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_TopLosers, R.drawable.ic_circle_down_20, getString(R.string.Market_SeeAll)) {
                    marketViewModel.onClickSeeAll(MarketModule.ListType.TopLosers)
                }
        )

        val topLosersAdapter = MarketOverviewItemsAdapter(
                this,
                marketOverviewViewModel.topLosersViewItemsLiveData,
                viewLifecycleOwner)

        val postsHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                hasItemsLiveData = Transformations.map(marketOverviewViewModel.postsViewItemsLiveData) { it.isNotEmpty() },
                viewLifecycleOwner = viewLifecycleOwner,
                settingsHeaderItem = MarketOverviewSectionHeaderAdapter.SectionHeaderItem(R.string.RateList_Posts, R.drawable.ic_post_20)
        )

        val postsAdapter = MarketPostItemsAdapter(
                this,
                marketOverviewViewModel.postsViewItemsLiveData,
                viewLifecycleOwner)


        val poweredByAdapter = PoweredByAdapter(marketOverviewViewModel.showPoweredByLiveData, viewLifecycleOwner, getString(R.string.Market_PoweredByApi))

        coinRatesRecyclerView.adapter = ConcatAdapter(
                marketMetricsAdapter,
                topLoadingAdapter,
                topGainersHeaderAdapter,
                topGainersAdapter,
                topLosersHeaderAdapter,
                topLosersAdapter,
                postsHeaderAdapter,
                postsAdapter,
                poweredByAdapter
        )
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketOverviewViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketMetricsViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

        marketOverviewViewModel.toastLiveData.observe(viewLifecycleOwner) {
            HudHelper.showErrorMessage(requireActivity().findViewById(android.R.id.content), it)
        }

        marketMetricsViewModel.showGlobalMarketMetricsPage.observe(viewLifecycleOwner, {
            MetricChartFragment.show(childFragmentManager, MetricChartType.MarketGlobal(it))
        })
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = CoinFragment.prepareParams(marketViewItem.coinType, marketViewItem.coinCode, marketViewItem.coinName)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    override fun onPostClick(postViewItem: MarketOverviewModule.PostViewItem) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        context?.let {
            customTabsIntent.launchUrl(it, Uri.parse(postViewItem.url))
        }
    }

}
