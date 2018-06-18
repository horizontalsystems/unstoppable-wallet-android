package bitcoin.wallet.modules.main

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import bitcoin.wallet.R
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

        bottomNavigation.defaultBackgroundColor = ContextCompat.getColor(this, R.color.appBackground)
        bottomNavigation.accentColor = ContextCompat.getColor(this, R.color.orange)
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
    }

}
