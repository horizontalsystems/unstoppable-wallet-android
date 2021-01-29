package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment(), FilterAdapter.Listener {
    private val filterAdapter = FilterAdapter(this)
    private val marketViewModel by viewModels<MarketViewModel> { MarketModule.Factory() }
    private val navigationViewModel by navGraphViewModels<MarketInternalNavigationViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTags.adapter = filterAdapter

        filterAdapter.setFilters(marketViewModel.tabs.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(marketViewModel.currentTab.name))

        viewPager.adapter = MarketTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        marketViewModel.tabLiveData.observe(viewLifecycleOwner, { tab: MarketService.Tab ->
            val contentFragment = when (tab) {
                MarketService.Tab.Overview -> 0
                MarketService.Tab.Discovery -> 1
                MarketService.Tab.Favorites -> 2
            }

            viewPager.setCurrentItem(contentFragment, false)
        })

        navigationViewModel.navigateToDiscoveryLiveEvent.observe(viewLifecycleOwner) {
            navigationViewModel.setDiscoveryMode(it)

            marketViewModel.currentTab = MarketService.Tab.Discovery
            filterAdapter.setFilters(marketViewModel.tabs.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(marketViewModel.currentTab.name))
        }
    }

    override fun updateFilter(query: String) {

    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {
        MarketService.Tab.fromString(item?.filterId)?.let {
            marketViewModel.currentTab = it
        }
    }
}
