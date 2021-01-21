package io.horizontalsystems.bankwallet.modules.market.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.market.top.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.modules.settings.main.SettingsMenuItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_rates.*

class MarketOverviewFragment : BaseFragment(), ViewHolderMarketTopItem.Listener {

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketOverviewViewModel by viewModels<MarketOverviewViewModel> { MarketOverviewModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)
        val topGainersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                SettingsMenuItem(R.string.RateList_TopWinners, R.drawable.ic_circle_up_20, value = getString(R.string.Market_SeeAll)) {

                }
        )
        val topGainersAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topGainersViewItemsLiveData, viewLifecycleOwner)

        val topLosersHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                SettingsMenuItem(R.string.RateList_TopLosers, R.drawable.ic_circle_down_20, value = getString(R.string.Market_SeeAll)) {

                }
        )
        val topLosersAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topLoosersViewItemsLiveData, viewLifecycleOwner)

        val topByVolumeHeaderAdapter = MarketOverviewSectionHeaderAdapter(
                SettingsMenuItem(R.string.RateList_TopByVolume, R.drawable.ic_chart_20, value = getString(R.string.Market_SeeAll)) {

                }
        )
        val topByVolumeAdapter = MarketOverviewItemsAdapter(this, marketOverviewViewModel.topByVolumeViewItemsLiveData, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketMetricsAdapter,
                topGainersHeaderAdapter,
                topGainersAdapter,
                topLosersHeaderAdapter,
                topLosersAdapter,
                topByVolumeHeaderAdapter,
                topByVolumeAdapter,
        )
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketOverviewViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }
    }

    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null, marketTopViewItem.coinType)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
