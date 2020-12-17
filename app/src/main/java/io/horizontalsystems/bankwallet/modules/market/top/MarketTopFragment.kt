package io.horizontalsystems.bankwallet.modules.market.top

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.modules.ratelist.*
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_rates.*

class MarketTopFragment : BaseFragment(), CoinRatesAdapter.Listener, CoinRatesSortingAdapter.Listener {

    private lateinit var coinRatesAdapter: CoinRatesAdapter
    private lateinit var coinRatesSortingAdapter: CoinRatesSortingAdapter
    private val viewModel by viewModels<MarketTopViewModel> { MarketTopModule.Factory() }
    private val presenter by viewModels<RateListPresenter> { RateListModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinRatesSortingAdapter = CoinRatesSortingAdapter(this)
        viewModel.sortingFieldLiveData.observe(viewLifecycleOwner, {
            coinRatesSortingAdapter.sortingFieldText = getString(it.titleResId)
        })
        viewModel.sortingPeriodLiveData.observe(viewLifecycleOwner, {
            coinRatesSortingAdapter.sortingPeriodText = getString(it.titleResId)
        })

        coinRatesAdapter = CoinRatesAdapter(this)

        coinRatesRecyclerView.adapter = ConcatAdapter(coinRatesSortingAdapter, coinRatesAdapter)

        presenter.viewDidLoad()
        observeView(presenter.view)
        observeRouter(presenter.router)
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


    override fun onResume() {
        super.onResume()
        presenter.loadTopList()
    }

    override fun onCoinClicked(coinItem: CoinItem) {
        presenter.onCoinClicked(coinItem)
    }

    private fun observeView(view: RateListView) {
        view.topViewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            if (viewItems.isNotEmpty()) {
                coinRatesAdapter.submitList(viewItems)
            }
        })
    }

    private fun observeRouter(router: RateListRouter) {
        router.openChartLiveEvent.observe(viewLifecycleOwner, Observer { (coinCode, coinTitle) ->
            val arguments = Bundle(3).apply {
                putString(RateChartFragment.COIN_CODE_KEY, coinCode)
                putString(RateChartFragment.COIN_TITLE_KEY, coinTitle)
                putString(RateChartFragment.COIN_ID_KEY, null)
            }

            findNavController().navigate(R.id.lockScreenFragment_to_rateChartFragment, arguments, navOptions())
        })

        router.openSortingTypeDialogLiveEvent.observe(viewLifecycleOwner, Observer { selected ->

            val sortTypes = listOf(TopListSortType.Rank, TopListSortType.Winners, TopListSortType.Losers)
            val selectorItems = sortTypes.map {
                SelectorItem(getString(it.titleRes), it == selected)
            }
            SelectorDialog
                    .newInstance(selectorItems, getString(R.string.Balance_Sort_PopupTitle)) { position ->
                        presenter.onTopListSortTypeChange(sortTypes[position])
                    }
                    .show(parentFragmentManager, "balance_sort_type_selector")

        })
    }
}
