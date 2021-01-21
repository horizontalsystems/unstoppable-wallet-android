package io.horizontalsystems.bankwallet.modules.market

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.market.favorites.MarketFavoritesFragment
import io.horizontalsystems.bankwallet.modules.market.overview.MarketOverviewFragment

class MarketTabsAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MarketOverviewFragment()
            1 -> MarketFavoritesFragment()
            else -> throw IllegalStateException()
        }
    }
}
