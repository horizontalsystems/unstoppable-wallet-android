package io.horizontalsystems.bankwallet.modules.market.top

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_rates.*
import java.util.*

class MarketTop100Fragment : BaseFragment(), CoinRatesSortingAdapter.Listener, MarketTopItemsAdapter.Listener {

    private lateinit var coinRatesSortingAdapter: CoinRatesSortingAdapter
    private lateinit var marketTopItemsAdapter: MarketTopItemsAdapter
    private lateinit var marketMetricsAdapter: MarketMetricsAdapter
    private lateinit var feeDataAdapter: FeeDataAdapter
    private val marketMetricsViewModel by viewModels<MarketMetricsViewModel> { MarketMetricsModule.Factory() }
    private val marketFeeViewModel by viewModels<MarketFeeViewModel> { MarketFeeModule.Factory() }
    private val viewModel by viewModels<MarketTopViewModel> { MarketTopModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        marketMetricsAdapter = MarketMetricsAdapter(marketMetricsViewModel, viewLifecycleOwner)
        feeDataAdapter = FeeDataAdapter()
        coinRatesSortingAdapter = CoinRatesSortingAdapter(this)
        marketTopItemsAdapter = MarketTopItemsAdapter(this, viewModel, viewLifecycleOwner)

        coinRatesRecyclerView.adapter = ConcatAdapter(marketMetricsAdapter, feeDataAdapter, coinRatesSortingAdapter, marketTopItemsAdapter)

        viewModel.sortingFieldLiveData.observe(viewLifecycleOwner, {
            coinRatesSortingAdapter.sortingFieldText = getString(it.titleResId)
        })
        viewModel.sortingPeriodLiveData.observe(viewLifecycleOwner, {
            coinRatesSortingAdapter.sortingPeriodText = getString(it.titleResId)
        })

        pullToRefresh.setOnRefreshListener {
            marketMetricsAdapter.refresh()
            marketTopItemsAdapter.refresh()

            pullToRefresh.isRefreshing = false
        }

        marketFeeViewModel.feeLiveData.observe(viewLifecycleOwner) {
            feeDataAdapter.submitList(listOf(Optional.of(it)))
        }
    }

    override fun onClickSortingField() {
        val items = viewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == viewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    viewModel.sortingField = viewModel.sortingFields[position]
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onClickSortingPeriod() {
        val items = viewModel.sortingPeriods.map {
            SelectorItem(getString(it.titleResId), it == viewModel.sortingPeriod)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Period_PopupTitle)) { position ->
                    viewModel.sortingPeriod = viewModel.sortingPeriods[position]
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
