package io.horizontalsystems.bankwallet.modules.market

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.market.top100.MarketTop100Fragment
import io.horizontalsystems.bankwallet.modules.market.top100.MarketTopFragment

class MarketTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MarketTopFragment()
            1 -> MarketTop100Fragment()
            2 -> MarketTop100Fragment()
            else -> throw IllegalStateException()
        }
    }
}
