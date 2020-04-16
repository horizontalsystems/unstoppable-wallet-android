package io.horizontalsystems.bankwallet.modules.main

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.rateapp.RateAppDialogFragment
import io.horizontalsystems.bankwallet.modules.send.SendActivity
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoView
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.views.helpers.LayoutHelper
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.main_activity_view_pager_layout.*

class MainActivity : BaseActivity(), TransactionInfoView.Listener, RateAppDialogFragment.Listener {

    private var adapter: MainTabsAdapter? = null
    private var disposables = CompositeDisposable()
    private var transInfoViewModel: TransactionInfoViewModel? = null
    private var txInfoBottomSheetBehavior: BottomSheetBehavior<View>? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    fun openTransactionInfo(txInfoItem: TransactionViewItem) {
        transInfoViewModel?.setViewItem(txInfoItem)
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

        viewPager.offscreenPageLimit = 2
        viewPager.setPagingEnabled(true)
        viewPager.adapter = adapter

        LayoutHelper.getAttr(R.attr.BottomNavigationBackgroundColor, theme)
                ?.let { ahBottomNavigation.defaultBackgroundColor = it }

        ahBottomNavigation.addItem(AHBottomNavigationItem(R.string.Balance_Title, R.drawable.bank_icon, 0))
        ahBottomNavigation.addItem(AHBottomNavigationItem(R.string.Transactions_Title, R.drawable.transactions, 0))
        ahBottomNavigation.addItem(AHBottomNavigationItem(R.string.Settings_Title, R.drawable.settings, 0))

        ahBottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_d)
        ahBottomNavigation.inactiveColor = ContextCompat.getColor(this, R.color.grey)
        ahBottomNavigation.titleState = AHBottomNavigation.TitleState.ALWAYS_HIDE
        ahBottomNavigation.setUseElevation(false)

        ahBottomNavigation.setOnTabSelectedListener { position, wasSelected ->
            if (!wasSelected) {
                viewPager.setCurrentItem(position, false)
            }
            true
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (positionOffset == 0f) {
                    adapter?.currentItem = ahBottomNavigation.currentItem
                }
            }

            override fun onPageSelected(position: Int) {
                ahBottomNavigation.currentItem = position
            }
        })

        val activeTab = intent?.extras?.getInt(ACTIVE_TAB_KEY)
        activeTab?.let { ahBottomNavigation.currentItem = it }

        preloadBottomSheets()
    }

    private fun preloadBottomSheets() {
        Handler().postDelayed({
            setBottomSheets()

            //transaction info
            txInfoBottomSheetBehavior = BottomSheetBehavior.from(transactionInfoNestedScrollView)
            setBottomSheet(txInfoBottomSheetBehavior)

            transInfoViewModel = ViewModelProvider(this).get(TransactionInfoViewModel::class.java)

            transInfoViewModel?.let {
                it.init()
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
        if (txInfoBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.txInfoCoinName)?.text?.isEmpty() == true) {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>?) {
        bottomSheetBehavior?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.isFitToContents = true
                    bottomSheetDim.alpha = 1f
                    bottomSheetDim.visibility = View.VISIBLE
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) {
                bottomSheetDim.alpha = slideOffset
                bottomSheetDim.visibility = if (slideOffset == 0f) View.GONE else View.VISIBLE
            }
        })
    }

    private fun hideDim() {
        bottomSheetDim.visibility = View.GONE
        bottomSheetDim.alpha = 0f
    }

    companion object {
        const val ACTIVE_TAB_KEY = "active_tab"
        const val SETTINGS_TAB_POSITION = 2
    }

}
