package io.horizontalsystems.bankwallet.modules.market

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.market.top.MarketTop100Fragment

class MarketTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MarketTop100Fragment()
            1 -> Fragment()
            2 -> Fragment()
            else -> throw IllegalStateException()
        }
    }
}
