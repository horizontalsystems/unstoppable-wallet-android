package bitcoin.wallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.LauncherActivity
import bitcoin.wallet.R
import bitcoin.wallet.blockchain.BlockchainManager
import bitcoin.wallet.core.App
import bitcoin.wallet.core.security.EncryptionManager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import kotlinx.android.synthetic.main.activity_dashboard.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MainTabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        adapter = MainTabsAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 2
        viewPager.setPagingEnabled(true)

        bottomNavigation.defaultBackgroundColor = ContextCompat.getColor(this, R.color.appBackground)
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

}
