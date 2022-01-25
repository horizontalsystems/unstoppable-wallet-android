package io.horizontalsystems.bankwallet.modules.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.balance2.BalanceXxxFragment
import io.horizontalsystems.bankwallet.modules.market.MarketFragment
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsFragment

class MainViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment.getChildFragmentManager(), fragment.viewLifecycleOwner.lifecycle) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int) = when (position) {
        0 -> MarketFragment()
        1 -> BalanceXxxFragment()
        2 -> TransactionsFragment()
        3 -> MainSettingsFragment()
        else -> throw IllegalStateException()
    }
}
