package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.modules.market.categories.MarketCategoriesModule
import io.horizontalsystems.bankwallet.modules.market.categories.MarketCategoriesService
import io.horizontalsystems.bankwallet.modules.market.categories.MarketCategoriesViewModel
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.core.navGraphViewModels
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment(), FilterAdapter.Listener {
    private val filterAdapter = FilterAdapter(this)
    private val viewModel by viewModels<MarketCategoriesViewModel> { MarketCategoriesModule.Factory() }
    private val navigationViewModel by navGraphViewModels<MarketInternalNavigationViewModel>(R.id.mainFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureSearchMenu(toolbar.menu, R.string.Market_Search)

        recyclerTags.adapter = filterAdapter

        filterAdapter.setFilters(viewModel.categories.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(viewModel.currentCategory.name))

        viewPager.adapter = MarketTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        viewModel.categoryLiveData.observe(viewLifecycleOwner, { category: MarketCategoriesService.Category ->
            val contentFragment = when (category) {
                MarketCategoriesService.Category.Overview -> 0
                MarketCategoriesService.Category.Discovery -> 1
                MarketCategoriesService.Category.Favorites -> 2
            }

            viewPager.setCurrentItem(contentFragment, false)
        })

        navigationViewModel.navigateToDiscoveryLiveEvent.observe(viewLifecycleOwner) {
            navigationViewModel.setDiscoveryMode(it)

            viewModel.currentCategory = MarketCategoriesService.Category.Discovery
            filterAdapter.setFilters(viewModel.categories.map { FilterAdapter.FilterItem(it.name) }, FilterAdapter.FilterItem(viewModel.currentCategory.name))
        }
    }

    override fun updateFilter(query: String) {

    }

    override fun onFilterItemClick(item: FilterAdapter.FilterItem?, itemPosition: Int, itemWidth: Int) {
        MarketCategoriesService.Category.fromString(item?.filterId)?.let {
            viewModel.currentCategory = it
        }
    }
}
