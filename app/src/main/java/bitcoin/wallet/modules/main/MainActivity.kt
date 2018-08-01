package bitcoin.wallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.View
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.R
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.App
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.viewHelpers.LayoutHelper
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import kotlinx.android.synthetic.main.activity_dashboard.*


class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MainTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lightMode = Factory.preferencesManager.isLightModeEnabled
        setTheme(if (lightMode) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)
        if (savedInstanceState != null) {
            setStatusBarIconColor(lightMode)
        }

        setContentView(R.layout.activity_dashboard)

        adapter = MainTabsAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.setPagingEnabled(true)

        bottomNavigation.defaultBackgroundColor = LayoutHelper.getAttrColor(R.attr.BottomNavigationBackgroundColor, theme)
        bottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_crypto)
        bottomNavigation.inactiveColor = ContextCompat.getColor(this, R.color.grey)

        bottomNavigation.addItem(AHBottomNavigationItem(R.string.tab_title_wallet, R.drawable.wallet, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.tab_title_transactions, R.drawable.transactions, 0))
        bottomNavigation.addItem(AHBottomNavigationItem(R.string.tab_title_settings, R.drawable.settings, 0))

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

        try {
            BlockchainManager.startServices()
        } catch (exception: UserNotAuthenticatedException) {
            EncryptionManager.showAuthenticationScreen(this, LauncherActivity.AUTHENTICATE_TO_REDIRECT)
        } catch (exception: KeyPermanentlyInvalidatedException) {
            EncryptionManager.showKeysInvalidatedAlert(this)
        }
    }

    override fun onResume() {
        super.onResume()

        if (App.promptPin) {
            startActivity(Intent(this, UnlockActivity::class.java))
            return
        }

        App.promptPin = false
    }

    private fun setStatusBarIconColor(lightMode: Boolean) {
        var flags = window.decorView.systemUiVisibility
        flags = if (lightMode) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // remove flag
        }
        window.decorView.systemUiVisibility = flags
    }

}
