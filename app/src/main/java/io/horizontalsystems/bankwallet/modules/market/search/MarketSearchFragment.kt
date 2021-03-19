package io.horizontalsystems.bankwallet.modules.market.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.market.favorites.EmptyListAdapter
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.inflate
import kotlinx.android.synthetic.main.fragment_market_search.*

class MarketSearchFragment : BaseFragment() {

    private val marketSearchViewModel by viewModels<MarketSearchViewModel> { MarketSearchModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        advancedSearch.setOnSingleClickListener {
            findNavController().navigate(R.id.marketSearchFragment_to_marketAdvancedSearchFragment, null, navOptions())
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

        val itemsAdapter = CoinDataItemsAdapter(::onItemClick)
        val emptyListAdapter = EmptyListAdapter(marketSearchViewModel.emptyResultsLiveData, viewLifecycleOwner) {  parent, viewType ->
            EmptyResultsViewHolder.create(parent, viewType)
        }

        rvItems.adapter = ConcatAdapter(itemsAdapter, emptyListAdapter)
        rvItems.itemAnimator = null

        marketSearchViewModel.itemsLiveData.observe(viewLifecycleOwner) {
            itemsAdapter.submitList(it)
        }

        marketSearchViewModel.advancedSearchButtonVisibleLiveDataViewItem.observe(viewLifecycleOwner) {
            advancedSearch.isVisible = it
        }
    }

    fun onItemClick(coinDataViewItem: CoinDataViewItem) {
        val arguments = CoinFragment.prepareParams(coinDataViewItem.type, coinDataViewItem.code, coinDataViewItem.name)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    class EmptyResultsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        companion object {
            fun create(parent: ViewGroup, viewType: Int): EmptyResultsViewHolder {
                return EmptyResultsViewHolder(inflate(parent, R.layout.view_holder_empty_results))
            }
        }
    }
}
