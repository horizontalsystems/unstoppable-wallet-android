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
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_rates.*

class MarketOverviewFragment : BaseFragment(), ViewHolderMarketTopItem.Listener {

    private lateinit var marketMetricsAdapter: MarketMetricsAdapter
    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketLoadingAdapter: MarketLoadingAdapter

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketTopViewModel by viewModels<MarketTopViewModel> { MarketTopModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)
        marketTopItemsAdapter = MarketTopItemsAdapter(this, marketTopViewModel.marketTopViewItemsLiveData, viewLifecycleOwner)
        marketLoadingAdapter = MarketLoadingAdapter(marketTopViewModel, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketMetricsAdapter, marketLoadingAdapter, marketTopItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketTopViewModel.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketTopViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })
    }

    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = RateChartFragment.prepareParams(marketTopViewItem.coinCode, marketTopViewItem.coinName, null, marketTopViewItem.coinType)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
