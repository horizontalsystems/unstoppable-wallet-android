package io.horizontalsystems.bankwallet.modules.market.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.market.*
import io.horizontalsystems.bankwallet.modules.market.list.MarketListViewModel
import io.horizontalsystems.bankwallet.modules.ratechart.RateChartFragment
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.android.synthetic.main.fragment_market_search.*

class MarketSearchFragment : BaseFragment(), ViewHolderMarketItem.Listener, MarketListHeaderView.Listener {

    private val factory by lazy { MarketSearchModule.Factory() }

    private val marketSearchViewModel by viewModels<MarketSearchViewModel> { factory }
    private val marketListViewModel by viewModels<MarketListViewModel> { factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)?.setBackgroundColor(Color.TRANSPARENT)
        searchView.findViewById<EditText>(R.id.search_src_text)?.let { editText ->
            context?.getColor(R.color.grey_50)?.let { color -> editText.setHintTextColor(color) }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newQuery: String): Boolean {
                marketSearchViewModel.query = newQuery.trim()
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = false
        })


        marketListHeader.listener = this
        marketListHeader.setSortingField(marketListViewModel.sortingField)
        marketListHeader.setMarketField(marketListViewModel.marketField)

        val marketItemsAdapter = MarketItemsAdapter(
                this,
                marketListViewModel.marketViewItemsLiveData,
                marketListViewModel.loadingLiveData,
                marketListViewModel.errorLiveData,
                viewLifecycleOwner
        )
        val marketLoadingAdapter = MarketLoadingAdapter(
                marketListViewModel.loadingLiveData,
                marketListViewModel.errorLiveData,
                marketListViewModel::onErrorClick,
                viewLifecycleOwner
        )

        coinRatesRecyclerView.adapter = ConcatAdapter(marketLoadingAdapter, marketItemsAdapter)
        coinRatesRecyclerView.itemAnimator = null

        marketListViewModel.networkNotAvailable.observe(viewLifecycleOwner, {
            HudHelper.showErrorMessage(requireView(), R.string.Hud_Text_NoInternet)
        })
    }

    override fun onItemClick(marketViewItem: MarketViewItem) {
        val arguments = RateChartFragment.prepareParams(marketViewItem.coinCode, marketViewItem.coinName, null)

        findNavController().navigate(R.id.rateChartFragment, arguments, navOptions())
    }

    override fun onClickSortingField() {
        val items = marketListViewModel.sortingFields.map {
            SelectorItem(getString(it.titleResId), it == marketListViewModel.sortingField)
        }

        SelectorDialog
                .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                    val selectedSortingField = marketListViewModel.sortingFields[position]

                    marketListHeader.setSortingField(selectedSortingField)
                    marketListViewModel.update(sortingField = selectedSortingField)
                }
                .show(childFragmentManager, "sorting_field_selector")
    }

    override fun onSelectMarketField(marketField: MarketField) {
        marketListViewModel.update(marketField = marketField)
    }
}
