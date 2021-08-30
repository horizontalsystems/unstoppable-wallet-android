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

        marketViewModel.currentTabLiveData.observe(viewLifecycleOwner) { tab: MarketModule.Tab ->
            val currentItemIndex = when (tab) {
                MarketModule.Tab.Overview -> 0
                MarketModule.Tab.Discovery -> 1
                MarketModule.Tab.Watchlist -> 2
            }

            setTabs(currentItemIndex)
            viewPager.setCurrentItem(currentItemIndex, false)
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

    private fun setTabs(currentItemIndex: Int) {
        val tabs = marketViewModel.tabs.map { getString(it.titleResId) }
        tabsCompose.setContent {
            ComposeAppTheme {
                Tabs(tabs, currentItemIndex) { index -> marketViewModel.onSelect(index) }
            }
        }
    }

    override fun updateFilter(query: String) {

    }

}
