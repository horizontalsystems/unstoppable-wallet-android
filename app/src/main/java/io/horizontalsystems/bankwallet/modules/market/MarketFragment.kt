package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment(), FilterAdapter.Listener {
    private val filterAdapter = FilterAdapter(this)
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment) { MarketModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTags.adapter = filterAdapter

        viewPager.adapter = MarketTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        marketViewModel.currentTabLiveData.observe(viewLifecycleOwner) { tab: MarketModule.Tab ->
            val currentItem = when (tab) {
                MarketModule.Tab.Overview -> 0
                MarketModule.Tab.Discovery -> 1
                MarketModule.Tab.Watchlist -> 2
            }

            viewPager.setCurrentItem(currentItem, false)

            filterAdapter.setFilters(marketViewModel.tabs.map { it.filterItem() }, tab.filterItem())
        }
    }

    private fun MarketModule.Tab.filterItem() = FilterAdapter.FilterItem(getString(titleResId))

    override fun updateFilter(query: String) {

    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {
        MarketModule.Tab.fromString(item?.filterId)?.let {
            marketViewModel.onSelect(it)
        }
    }
}
