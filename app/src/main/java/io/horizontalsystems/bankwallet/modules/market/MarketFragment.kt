package io.horizontalsystems.bankwallet.modules.market

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseWithSearchFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Grey
import io.horizontalsystems.bankwallet.ui.compose.Steel10
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
        tabsCompose.setContent {
            var tabIndex by remember { mutableStateOf(currentItemIndex) }
            ComposeAppTheme {
                Column {
                    TabRow(
                        modifier = Modifier.height(44.dp),
                        selectedTabIndex = tabIndex,
                        backgroundColor = ComposeAppTheme.colors.tyler,
                        contentColor = ComposeAppTheme.colors.tyler,
                        indicator = @Composable { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                                color = ComposeAppTheme.colors.jacob
                            )
                        }
                    ) {
                        marketViewModel.tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = tabIndex == index,
                                onClick = {
                                    tabIndex = index
                                    marketViewModel.onSelect(index)
                                },
                                text = {
                                    ProvideTextStyle(
                                        ComposeAppTheme.typography.subhead1.copy(
                                            textAlign = TextAlign.Center
                                        )
                                    ) {
                                        Text(
                                            text = tab.name,
                                            color = if (tabIndex == index) ComposeAppTheme.colors.oz else Grey
                                        )
                                    }
                                })
                        }
                    }
                    Divider(thickness = 1.dp, color = Steel10)
                }
            }
        }
    }

    override fun updateFilter(query: String) {

    }

}
