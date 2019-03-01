package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.WindowManager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_dashboard.*

class MainActivity : BaseActivity() {

    private lateinit var adapter: MainTabsAdapter
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        adapter = MainTabsAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        setSwipeEnabled(true)

        LayoutHelper.getAttr(R.attr.BottomNavigationBackgroundColor, theme)?.let {
            bottomNavigation.defaultBackgroundColor = it
        }
        bottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_crypto)
        bottomNavigation.inactiveColor = ContextCompat.getColor(this, R.color.grey)

        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Balance_Title, R.drawable.bank_icon, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Transactions_Title, R.drawable.transactions, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.Settings_Title, R.drawable.settings, 0))
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

        disposable = App.appCloseManager.appCloseSignal.subscribe {
            moveTaskToBack(false)
        }
    }

    override fun onBackPressed() {
        if (adapter.currentItem == 1 && adapter.getTransactionFragment().onBackPressed()) {
            return
        } else if (adapter.currentItem > 0) {
            viewPager.currentItem = 0
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    fun setBottomNavigationVisible(visible: Boolean) {
        bottomNavigation.animate().translationY(if (visible) 0f else (bottomNavigation.height).toFloat()).duration = 150
        bottomNavigationBarShadow.animate().translationY(if (visible) 0f else (bottomNavigation.height).toFloat()).duration = 150
    }

    fun updateSettingsTabCounter(count: Int) {
        val countText = if (count < 1) "" else count.toString()
        val settingsTabPosition = 2
        bottomNavigation.setNotification(countText, settingsTabPosition)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewPager.setPagingEnabled(enabled)
    }

    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 2
    }

}
