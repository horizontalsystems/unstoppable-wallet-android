package io.horizontalsystems.bankwallet.modules.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoView
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.main_activity_view_pager_layout.*

class MainActivity : BaseActivity(), TransactionInfoView.Listener, RateAppDialogFragment.Listener {

    private var adapter: MainTabsAdapter? = null
    private var disposables = CompositeDisposable()
    private var transInfoViewModel: TransactionInfoViewModel? = null
    private var txInfoBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var messageInfoSnackbar: Snackbar? = null
    private var bottomBadgeView: View? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null) //null prevents fragments restoration on theme switch

        setContentView(R.layout.activity_dashboard)
        setTransparentStatusBar()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.init()

        viewModel.showRateAppLiveEvent.observe(this, Observer {
            RateAppDialogFragment.show(this, this)
        })

        viewModel.hideContentLiveData.observe(this, Observer { hide ->
            screenSecureDim.isVisible = hide
        })

        viewModel.setBadgeVisibleLiveData.observe(this, Observer { visible ->
            val bottomMenu = bottomNavigation.getChildAt(0) as? BottomNavigationMenuView
            val settingsNavigationViewItem = bottomMenu?.getChildAt(3) as? BottomNavigationItemView

            if (visible) {
                if(bottomBadgeView?.parent == null) {
                    settingsNavigationViewItem?.addView(getBottomBadge())
                }
            } else {
                settingsNavigationViewItem?.removeView(bottomBadgeView)
            }
        })

        adapter = MainTabsAdapter(supportFragmentManager)

        findViewById<ViewStub>(R.id.viewPagerStub)?.let {
            it.inflate()
            loadViewPager()
        }
    }

    override fun onBackPressed() = when {
        txInfoBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED -> {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        (adapter?.currentItem ?: 0) > 0 -> {
            viewPager.currentItem = 0
        }
        else -> super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        viewModel.delegate.onResume()
        collapseBottomSheetsOnActivityRestore()
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    fun openSend(wallet: Wallet) {
        val intent = Intent(this, SendActivity::class.java).apply {
            putExtra(SendActivity.WALLET, wallet)
        }
        startActivity(intent)
    }

    /***
    TransactionInfo bottomsheet
     */

    fun openTransactionInfo(transactionRecord: TransactionRecord, wallet: Wallet) {
        transInfoViewModel?.init(transactionRecord, wallet)
    }

    override fun openFullTransactionInfo(transactionHash: String, wallet: Wallet) {
        FullTransactionInfoModule.start(this, transactionHash, wallet)
    }

    override fun openTransactionInfo() {
        txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun closeTransactionInfo() {
        txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onShowInfoMessage(snackbar: Snackbar?) {
        this.messageInfoSnackbar = snackbar
    }

    // RateAppDialogFragment.Listener

    override fun onClickRateApp() {
        openAppInPlayStore()
    }

    override fun onClickCancel() {
    }

    override fun onDismiss() {
    }

    private fun openAppInPlayStore() {
        val uri = Uri.parse("market://details?id=io.horizontalsystems.bankwallet")  //context.packageName
        val goToMarketIntent = Intent(Intent.ACTION_VIEW, uri)

        val flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        goToMarketIntent.addFlags(flags)

        try {
            ContextCompat.startActivity(this, goToMarketIntent, null)
        } catch (e: ActivityNotFoundException) {

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=io.horizontalsystems.bankwallet"))

            ContextCompat.startActivity(this, intent, null)
        }
    }

    private fun loadViewPager() {
        setTopMarginByStatusBarHeight(viewPager)

        viewPager.offscreenPageLimit = 3
        viewPager.adapter = adapter

        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            val index = getBottomMenuItemIndex(menuItem.itemId)
            viewPager.setCurrentItem(index, false)
            true
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset == 0f) {
                    adapter?.currentItem = getBottomMenuItemIndex(bottomNavigation.selectedItemId)
                }
            }

            override fun onPageSelected(position: Int) {
                val itemId = getBottomNavigationItemId(position)
                bottomNavigation.menu.findItem(itemId).isChecked = true
            }
        })

        val activeTab = intent?.extras?.getInt(ACTIVE_TAB_KEY)
        activeTab?.let { position ->
            bottomNavigation.selectedItemId = getBottomNavigationItemId(position)
        }

        preloadBottomSheets()
    }

    private fun getBottomBadge(): View? {
        if (bottomBadgeView != null){
            return bottomBadgeView
        }

        val bottomMenu = bottomNavigation.getChildAt(0) as? BottomNavigationMenuView

        bottomBadgeView = LayoutInflater.from(this).inflate(R.layout.view_bottom_navigation_badge,
                bottomMenu,false)

        return bottomBadgeView
    }

    private fun getBottomNavigationItemId(position: Int): Int {
        return when (position) {
            0 -> R.id.navigation_balance
            1 -> R.id.navigation_transactions
            2 -> R.id.navigation_guides
            3 -> R.id.navigation_settings
            else -> throw Exception("BottomNavigation position exceeded")
        }
    }

    private fun getBottomMenuItemIndex(itemId: Int): Int {
        return when (itemId) {
            R.id.navigation_balance -> 0
            R.id.navigation_transactions -> 1
            R.id.navigation_guides -> 2
            R.id.navigation_settings -> 3
            else -> throw Exception("No such BottomNavigation page")
        }
    }

    private fun preloadBottomSheets() {
        Handler().postDelayed({
            setBottomSheets()

            //transaction info
            txInfoBottomSheetBehavior = BottomSheetBehavior.from(transactionInfoNestedScrollView)
            setBottomSheet(txInfoBottomSheetBehavior)

            transInfoViewModel = ViewModelProvider(this).get(TransactionInfoViewModel::class.java)

            transInfoViewModel?.let {
                transactionInfoView.bind(it, this, this)
            }
        }, 200)
    }

    private fun setBottomSheets() {
        hideDim()

        bottomSheetDim.setOnClickListener {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun collapseBottomSheetsOnActivityRestore() {
        if (txInfoBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.secondaryName)?.text?.isEmpty() == true) {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>?) {
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.isFitToContents = true
                    bottomSheetDim.alpha = 1f
                    bottomSheetDim.isVisible = true
                }
                else{
                    messageInfoSnackbar?.dismiss()
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                bottomSheetDim.alpha = slideOffset
                bottomSheetDim.isGone = slideOffset == 0f
            }
        })
    }

    private fun hideDim() {
        bottomSheetDim.isGone = true
        bottomSheetDim.alpha = 0f
    }

    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 3
    }

}
