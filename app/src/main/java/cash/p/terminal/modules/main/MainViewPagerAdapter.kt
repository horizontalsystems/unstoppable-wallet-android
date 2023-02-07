package cash.p.terminal.modules.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import cash.p.terminal.modules.balance.BalanceFragment
import cash.p.terminal.modules.market.MarketFragment
import cash.p.terminal.modules.settings.main.MainSettingsFragment
import cash.p.terminal.modules.transactions.TransactionsFragment

class MainViewPagerAdapter(fragment: Fragment, private var marketsTabEnabled: Boolean) : FragmentStateAdapter(fragment.getChildFragmentManager(), fragment.viewLifecycleOwner.lifecycle) {

    private val marketsTabPosition = 2
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
        0 -> BalanceFragment()
        1 -> TransactionsFragment()
        2 -> if (marketsTabEnabled) {
            MarketFragment()
        } else {
            Fragment()
        }
        3 -> MainSettingsFragment()
        else -> throw IllegalStateException()
    }
}
