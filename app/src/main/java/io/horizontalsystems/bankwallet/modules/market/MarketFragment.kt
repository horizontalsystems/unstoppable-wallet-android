package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.market.tabs.MarketTabsModule
import io.horizontalsystems.bankwallet.modules.market.tabs.MarketTabsService
import io.horizontalsystems.bankwallet.modules.market.tabs.MarketTabsViewModel
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment(), FilterAdapter.Listener {
    private val filterAdapter = FilterAdapter(this)
    private val marketTabsViewModel by viewModels<MarketTabsViewModel> { MarketTabsModule.Factory() }
    private val navigationViewModel by navGraphViewModels<MarketInternalNavigationViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerTags.adapter = filterAdapter

        filterAdapter.setFilters(marketTabsViewModel.tabs.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(marketTabsViewModel.currentTab.name))

        viewPager.adapter = MarketTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        marketTabsViewModel.tabLiveData.observe(viewLifecycleOwner, { tab: MarketTabsService.Tab ->
            val contentFragment = when (tab) {
                MarketTabsService.Tab.Overview -> 0
                MarketTabsService.Tab.Discovery -> 1
                MarketTabsService.Tab.Favorites -> 2
            }

            viewPager.setCurrentItem(contentFragment, false)
        })

        navigationViewModel.navigateToDiscoveryLiveEvent.observe(viewLifecycleOwner) {
            navigationViewModel.setDiscoveryMode(it)

            marketTabsViewModel.currentTab = MarketTabsService.Tab.Discovery
            filterAdapter.setFilters(marketTabsViewModel.tabs.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(marketTabsViewModel.currentTab.name))
        }
    }

    override fun updateFilter(query: String) {

    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {
        MarketTabsService.Tab.fromString(item?.filterId)?.let {
            marketTabsViewModel.currentTab = it
        }
    }
}
