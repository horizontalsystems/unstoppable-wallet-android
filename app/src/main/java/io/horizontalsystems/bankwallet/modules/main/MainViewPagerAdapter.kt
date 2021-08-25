package io.horizontalsystems.bankwallet.modules.main

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.horizontalsystems.bankwallet.modules.balance.BalanceFragment
import io.horizontalsystems.bankwallet.modules.balance.BalanceNoCoinsFragment
import io.horizontalsystems.bankwallet.modules.balanceonboarding.BalanceOnboardingViewModel.BalanceViewType
import io.horizontalsystems.bankwallet.modules.market.MarketFragment
import io.horizontalsystems.bankwallet.modules.onboarding.OnboardingFragment
import io.horizontalsystems.bankwallet.modules.settings.main.MainSettingsFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsFragment

class MainViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment.getChildFragmentManager(), fragment.viewLifecycleOwner.lifecycle) {

    private val itemIds = mutableListOf(0, getBalancePageId(BalanceViewType.Balance), 2, 3)

    var balancePageType: BalanceViewType = BalanceViewType.Balance
        set(value) {
            if (field == value) return
            field = value

            itemIds[1] = getBalancePageId(value)
            notifyItemChanged(1)
        }

    override fun containsItem(itemId: Long) = itemIds.contains(itemId)
    override fun getItemId(position: Int) = itemIds[position]
    override fun getItemCount() = 4

    override fun createFragment(position: Int) = when (position) {
        0 -> MarketFragment()
        1 -> when (val tmp = balancePageType) {
            BalanceViewType.NoAccounts -> OnboardingFragment()
            is BalanceViewType.NoCoins -> BalanceNoCoinsFragment(tmp.accountName)
            BalanceViewType.Balance -> BalanceFragment()
        }
        2 -> TransactionsFragment()
        3 -> MainSettingsFragment()
        else -> throw IllegalStateException()
    }

    private fun getBalancePageId(type: BalanceViewType): Long = when (type) {
        BalanceViewType.NoAccounts -> 10
        BalanceViewType.Balance -> 11
        is BalanceViewType.NoCoins -> 12
    }

}
