package bitcoin.wallet.modules.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.R
import bitcoin.wallet.modules.settings.SettingsFragment
import bitcoin.wallet.modules.transactions.TransactionsFragment
import bitcoin.wallet.modules.wallet.WalletFragment
import kotlinx.android.synthetic.main.activity_dashboard.*

class MainActivity : AppCompatActivity() {

    enum class FragmentTag {
        WALLET, TRANSACTIONS, SETTINGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        setSupportActionBar(toolbar)

        // todo move title to fragments
        supportActionBar?.title = "Balance"

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

            val fragment = getFragmentByTag(fragmentTag)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fl_main, fragment, fragmentTag.name)
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