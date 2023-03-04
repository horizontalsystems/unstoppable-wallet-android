package io.horizontalsystems.bankwallet.modules.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.market.MarketFragment
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsFragment

class MainViewPagerAdapter(fragment: Fragment, private var marketsTabEnabled: Boolean) : FragmentStateAdapter(fragment.getChildFragmentManager(), fragment.viewLifecycleOwner.lifecycle) {

    private val marketsTabPosition = 0
    private val marketsTabIdEnabled = 10L
    private val marketsTabIdDisabled = 11L

    fun setMarketsTabEnabled(value: Boolean) {
        if (marketsTabEnabled == value) return
        marketsTabEnabled = value

        notifyItemChanged(0)
    }

    override fun containsItem(itemId: Long) : Boolean {
        return when (itemId) {
            marketsTabIdEnabled -> marketsTabEnabled
            marketsTabIdDisabled -> !marketsTabEnabled
            else -> super.containsItem(itemId)
        }
    }
    override fun getItemId(position: Int): Long {
        return if (position != marketsTabPosition) {
            super.getItemId(position)
        } else if (marketsTabEnabled) {
            marketsTabIdEnabled
        } else {
            marketsTabIdDisabled
        }
    }
    override fun getItemCount() = 4

    override fun createFragment(position: Int) = when (position) {
        0 -> if (marketsTabEnabled) {
            MarketFragment()
        } else {
            Fragment()
        }
        1 -> BalanceFragment()
        2 -> TransactionsFragment()
        3 -> MainSettingsFragment()
        else -> throw IllegalStateException()
    }
}
