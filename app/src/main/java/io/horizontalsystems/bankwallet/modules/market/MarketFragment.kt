package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.bankwallet.ui.compose.components.Tabs
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_market.*

class MarketFragment : BaseWithSearchFragment() {
    private val marketViewModel by navGraphViewModels<MarketViewModel>(R.id.mainFragment) { MarketModule.Factory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_market, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager.adapter = MarketTabsAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.isUserInputEnabled = false

        marketViewModel.selectedTab.observe(viewLifecycleOwner) { selectedTab ->
            setTabs(selectedTab)
            viewPager.setCurrentItem(marketViewModel.tabs.indexOf(selectedTab), false)
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.search -> {
                    findNavController().navigate(R.id.mainFragment_to_marketSearchFragment)
                    true
                }
                else -> false
            }
        }

        tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    private fun setTabs(selectedTab: MarketModule.Tab) {
        val tabItems = marketViewModel.tabs.map {
            TabItem(getString(it.titleResId), it == selectedTab, it)
        }
        tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabItems) { item ->
                    marketViewModel.onSelect(item)
                }
            }
        }
    }

    override fun updateFilter(query: String) {

    }

}
