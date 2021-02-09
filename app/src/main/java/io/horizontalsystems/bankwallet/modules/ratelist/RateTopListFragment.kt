package io.horizontalsystems.bankwallet.modules.ratelist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.core.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import kotlinx.android.synthetic.main.fragment_rates.*

class RatesTopListFragment : BaseFragment(), CoinRatesAdapter.Listener {

    private lateinit var coinRatesHeaderAdapter: CoinRatesHeaderAdapter
    private lateinit var coinRatesAdapter: CoinRatesAdapter
    private lateinit var sourceAdapter: SourceAdapter
    private val presenter: RateListPresenter by activityViewModels { RateListModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_rates, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        coinRatesHeaderAdapter = CoinRatesHeaderAdapter(true, getString(R.string.RateList_top100), View.OnClickListener {
            presenter.onTopListSortClick()
        })
        coinRatesAdapter = CoinRatesAdapter(this)

        sourceAdapter = SourceAdapter(false)

        coinRatesRecyclerView.itemAnimator = null
        coinRatesRecyclerView.adapter = ConcatAdapter(coinRatesHeaderAdapter, coinRatesAdapter, sourceAdapter)

        presenter.viewDidLoad()
        observeView(presenter.view)
        observeRouter(presenter.router)
    }

    override fun onResume() {
        super.onResume()
        presenter.loadTopList()
    }

    override fun onCoinClicked(coinItem: CoinItem) {
        presenter.onCoinClicked(coinItem)
    }

    private fun observeView(view: RateListView) {
        view.datesLiveData.observe(viewLifecycleOwner, Observer { lastUpdateTimestamp ->
            coinRatesHeaderAdapter.timestamp = lastUpdateTimestamp
        })

        view.topViewItemsLiveData.observe(viewLifecycleOwner, Observer { viewItems ->
            if (viewItems.isNotEmpty()) {
                coinRatesHeaderAdapter.showSpinner = false
                coinRatesHeaderAdapter.notifyDataSetChanged()

                coinRatesAdapter.submitList(viewItems) {
                    sourceAdapter.visible = true
                    sourceAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    private fun observeRouter(router: RateListRouter) {
        router.openChartLiveEvent.observe(viewLifecycleOwner, Observer { (coinCode, coinTitle) ->
            val arguments = RateChartFragment.prepareParams(coinCode, coinTitle, null)

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
