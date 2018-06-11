package org.grouvi.wallet.modules.dashboard

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_dashboard.*
import org.grouvi.wallet.R
import org.grouvi.wallet.modules.settings.SettingsFragment
import org.grouvi.wallet.modules.transactions.TransactionsFragment
import org.grouvi.wallet.modules.wallet.WalletFragment

class DashboardActivity : AppCompatActivity() {

    enum class FragmentTag {
        WALLET, TRANSACTIONS, SETTINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        bottomNavigation.setOnNavigationItemSelectedListener {
            val fragmentTag = when (it.itemId) {
                R.id.action_wallet -> {
                    FragmentTag.WALLET
                }
                R.id.action_transactions -> {
                    FragmentTag.TRANSACTIONS
                }
                R.id.action_settings -> {
                    FragmentTag.SETTINGS
                }
                else -> {
                    FragmentTag.WALLET
                }
            }

            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fl_main, getFragmentByTag(fragmentTag), fragmentTag.name)
                    .commit()

            true
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fl_main, getFragmentByTag(FragmentTag.WALLET), FragmentTag.WALLET.name)
                .commit()


    }

    private fun getFragmentByTag(fragmentTag: FragmentTag): Fragment {
        return supportFragmentManager.findFragmentByTag(fragmentTag.name) ?: when (fragmentTag) {
            FragmentTag.WALLET -> {
                WalletFragment()
            }
            FragmentTag.TRANSACTIONS -> {
                TransactionsFragment()
            }
            FragmentTag.SETTINGS -> {
                SettingsFragment()
            }
        }
    }
}