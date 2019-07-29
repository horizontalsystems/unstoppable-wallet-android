package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.view.ViewStub
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.aurelhubert.ahbottomnavigation.AHBottomNavigation
import com.aurelhubert.ahbottomnavigation.AHBottomNavigationItem
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.zxing.integration.android.IntentIntegrator
import io.horizontalsystems.bankwallet.BaseActivity
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import io.horizontalsystems.bankwallet.modules.qrscanner.QRScannerModule
import io.horizontalsystems.bankwallet.modules.receive.ReceiveView
import io.horizontalsystems.bankwallet.modules.receive.ReceiveViewModel
import io.horizontalsystems.bankwallet.modules.send.ConfirmationFragment
import io.horizontalsystems.bankwallet.modules.send.SendView
import io.horizontalsystems.bankwallet.modules.send.SendViewModel
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoView
import io.horizontalsystems.bankwallet.modules.transactions.transactionInfo.TransactionInfoViewModel
import io.horizontalsystems.bankwallet.viewHelpers.LayoutHelper
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.main_activity_view_pager_layout.*

class MainActivity : BaseActivity(), SendView.Listener, ReceiveView.Listener, TransactionInfoView.Listener {

    private var adapter: MainTabsAdapter? = null
    private var disposables = CompositeDisposable()
    private var receiveViewModel: ReceiveViewModel? = null
    private var sendViewModel: SendViewModel? = null
    private var transInfoViewModel: TransactionInfoViewModel? = null
    private var receiveBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var sendBottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var txInfoBottomSheetBehavior: BottomSheetBehavior<View>? = null

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)
        setTransparentStatusBar()

        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        viewModel.init()

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
        receiveBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED -> {
            receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        sendBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED -> {
            sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        (adapter?.currentItem ?: 0) > 0 -> {
            viewPager.currentItem = 0
        }
        else -> super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        sendViewModel?.onViewResumed()
        collapseBottomSheetsOnActivityRestore()
    }

    override fun onDestroy() {
        disposables.dispose()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (scanResult != null && !TextUtils.isEmpty(scanResult.contents)) {
            sendViewModel?.delegate?.onScanAddress(scanResult.contents)
        }
    }

    /***
    Receive bottomsheet
     */

    fun openReceiveDialog(coinCode: String) {
        receiveViewModel?.init(coinCode)
    }

    override fun expandReceiveDialog() {
        receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun closeReceiveDialog() {
        receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        hideDim()
    }

    override fun shareReceiveAddress(address: String) {
        ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(address)
                .startChooser()
    }

    /***
    Send bottomsheet
     */

    fun openSendDialog(coinCode: String) {
        sendViewModel?.let {
            it.init(coinCode)
            sendView.update()
        }
        sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun closeSend() {
        sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        hideDim()
    }

    override fun openSendScanner() {
        QRScannerModule.start(this)
    }

    override fun showSendConfirmationDialog() {
        ConfirmationFragment.show(this)
    }

    /***
    TransactionInfo bottomsheet
     */

    fun openTransactionInfo(txInfoItem: TransactionViewItem) {
        transInfoViewModel?.setViewItem(txInfoItem)
    }

    override fun openFullTransactionInfo(transactionHash: String, coin: Coin) {
        FullTransactionInfoModule.start(this, transactionHash, coin)
    }

    override fun opemTransactionInfo() {
        txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
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

        ahBottomNavigation.accentColor = ContextCompat.getColor(this, R.color.yellow_crypto)
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
            //receive
            receiveBottomSheetBehavior = BottomSheetBehavior.from(receiveNestedScrollView)
            setBottomSheet(receiveBottomSheetBehavior)

            receiveViewModel = ViewModelProviders.of(this).get(ReceiveViewModel::class.java)
            receiveViewModel?.let {
                receiveView.bind(it, this, this)
            }

            //send
            sendBottomSheetBehavior = BottomSheetBehavior.from(sendNestedScrollView)
            setBottomSheet(sendBottomSheetBehavior)

            sendViewModel = ViewModelProviders.of(this).get(SendViewModel::class.java)
            sendViewModel?.let { sendView.initView(it, this, this) }

            //transaction info
            txInfoBottomSheetBehavior = BottomSheetBehavior.from(transactionInfoNestedScrollView)
            setBottomSheet(txInfoBottomSheetBehavior)

            transInfoViewModel = ViewModelProviders.of(this).get(TransactionInfoViewModel::class.java)

            transInfoViewModel?.let {
                it.init()
                transactionInfoView.bind(it, this, this)
            }
        }, 200)
    }

    private fun setBottomSheets() {
        hideDim()

        bottomSheetDim.setOnClickListener {
            receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    fun updateSettingsTabCounter(count: Int) {
        val countText = if (count < 1) "" else "!"
        val settingsTabPosition = 2
        findViewById<AHBottomNavigation>(R.id.ahBottomNavigation)?.setNotification(countText, settingsTabPosition)
    }

    private fun collapseBottomSheetsOnActivityRestore() {
        if (receiveBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.receiveTxtTitle)?.text?.isEmpty() == true) {
            receiveBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (sendBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.sendTxtTitle)?.text?.isEmpty() == true) {
            sendBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        if (txInfoBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED && findViewById<TextView>(R.id.txInfoCoinName)?.text?.isEmpty() == true) {
            txInfoBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun setBottomSheet(bottomSheetBehavior: BottomSheetBehavior<View>?) {
        bottomSheetBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.isFitToContents = true
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
