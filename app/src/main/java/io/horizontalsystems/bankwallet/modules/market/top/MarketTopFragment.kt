package io.horizontalsystems.bankwallet.modules.market.top

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.fee.MarketFeeDataAdapter
import io.horizontalsystems.bankwallet.modules.market.fee.MarketFeeModule
import io.horizontalsystems.bankwallet.modules.market.fee.MarketFeeViewModel
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsAdapter
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsModule
import io.horizontalsystems.bankwallet.modules.market.metrics.MarketMetricsViewModel
import io.horizontalsystems.bankwallet.modules.market.top100.*
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_rates.*
import java.util.*

class MarketTopFragment : BaseFragment(), MarketTop100HeaderAdapter.Listener, MarketTop100ItemsAdapter.Listener {

    private lateinit var marketMetricsAdapter: MarketMetricsAdapter
    private lateinit var marketFeeDataAdapter: MarketFeeDataAdapter
    private lateinit var marketTopHeaderAdapter: MarketTop100HeaderAdapter
    private lateinit var marketTop100ItemsAdapter: MarketTop100ItemsAdapter

    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketFeeViewModel by viewModels<MarketFeeViewModel> { MarketFeeModule.Factory() }
    private val marketTopViewModel by viewModels<MarketTop100ViewModel> { MarketTop100Module.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)
        marketFeeDataAdapter = MarketFeeDataAdapter()
        marketTopHeaderAdapter = MarketTop100HeaderAdapter(this, marketTopViewModel, viewLifecycleOwner)
        marketTop100ItemsAdapter = MarketTop100ItemsAdapter(this, marketTopViewModel, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketMetricsAdapter, marketFeeDataAdapter, marketTopHeaderAdapter, marketTop100ItemsAdapter)

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketTop100ItemsAdapter.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketFeeViewModel.feeLiveData.observe(viewLifecycleOwner) {
            marketFeeDataAdapter.submitList(listOf(Optional.of(it)))
        }
    }

    override fun onClickSortingField() {
        val items = marketTopViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketTopViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    marketTopViewModel.sortingField = marketTopViewModel.sortingFields[position]
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onClickPeriod() {
        val items = marketTopViewModel.periods.map {
            SelectorItem(getString(it.titleResId), it == marketTopViewModel.period)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Period_PopupTitle)) { position ->
                    marketTopViewModel.period = marketTopViewModel.periods[position]
                }
                .show(childFragmentManager, "sorting_period_selector")
    }


    override fun onItemClick(marketTopViewItem: MarketTopViewItem) {
        val arguments = Bundle(3).apply {
            putString(RateChartFragment.COIN_CODE_KEY, marketTopViewItem.coinCode)
            putString(RateChartFragment.COIN_TITLE_KEY, marketTopViewItem.coinName)
            putString(RateChartFragment.COIN_ID_KEY, null)
        }

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }
}
