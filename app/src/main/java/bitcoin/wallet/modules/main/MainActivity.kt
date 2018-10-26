package bitcoin.wallet.modules.main

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import bitcoin.wallet.BaseActivity
import bitcoin.wallet.R
import bitcoin.wallet.core.App
import bitcoin.wallet.viewHelpers.LayoutHelper
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import kotlinx.android.synthetic.main.activity_dashboard.*

class MainActivity : BaseActivity() {

    private lateinit var adapter: MainTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        adapter = MainTabsAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.setPagingEnabled(true)

        LayoutHelper.getAttr(R.attr.BottomNavigationBackgroundColor, theme)?.let {
            bottomNavigation.defaultBackgroundColor = it
        }
        bottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_crypto)
        bottomNavigation.inactiveColor = ContextCompat.getColor(this, R.color.grey)

        bottomNavigation.addItem(AHBottomNavigationItem(R.string.wallet_title, R.drawable.bank_icon, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.transactions_title, R.drawable.transactions, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.settings_title, R.drawable.settings, 0))
        bottomNavigation.titleState = AHBottomNavigation.TitleState.ALWAYS_HIDE
        bottomNavigation.setUseElevation(false)

        bottomNavigation.setOnTabSelectedListener { position, wasSelected ->
            if (!wasSelected) {
                viewPager.setCurrentItem(position, false)
            }
            true
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset == 0f) {
                    adapter.currentItem = bottomNavigation.currentItem
                }
            }

            override fun onPageSelected(position: Int) {
                bottomNavigation.currentItem = position
            }
        })

        val activeTab = intent?.extras?.getInt(ACTIVE_TAB_KEY)
        activeTab?.let {
            bottomNavigation.currentItem = it
        }
    }

    override fun onResume() {
        super.onResume()
        if (App.adapterManager.adapters.isEmpty()) {
            safeExecuteWithKeystore(
                    action = Runnable {
                        App.wordsManager.safeLoad()
                    },
                    onSuccess = Runnable {
                        App.adapterManager.start()
                    }
            )
        }
    }

    fun updateSettingsTabCounter(count: Int) {
        val countText = if (count < 1) "" else count.toString()
        val settingsTabPosition = 2
        bottomNavigation.setNotification(countText, settingsTabPosition)
    }

    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 2
    }

}
