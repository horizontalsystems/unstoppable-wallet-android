package bitcoin.wallet.modules.main

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.util.SparseArray
import android.view.ViewGroup
import bitcoin.wallet.modules.settings.SettingsFragment
import bitcoin.wallet.modules.transactions.TransactionsFragment
import bitcoin.wallet.modules.wallet.WalletFragment

class MainTabsAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    var currentItem = 0

    private val registeredFragments = SparseArray<Fragment>()

    override fun getCount(): Int = 3

    override fun getItem(position: Int): Fragment = when (position) {
        0 -> WalletFragment()
        1 -> TransactionsFragment()
        else -> SettingsFragment()
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        registeredFragments.put(position, fragment)
        return fragment
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        registeredFragments.remove(position)
        super.destroyItem(container, position, `object`)
    }

}
