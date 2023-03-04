package io.horizontalsystems.bankwallet.modules.main

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WC2RequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2MainViewModel
import io.horizontalsystems.core.CoreApp

class MainActivity : BaseActivity() {

    private val wc2MainViewModel by viewModels<WC2MainViewModel> {
        WC2MainViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navHost.navController.setGraph(R.navigation.main_graph, intent.extras)
        navHost.navController.addOnDestinationChangedListener(this)

        wc2MainViewModel.sessionProposalLiveEvent.observe(this) {
            navHost.navController.slideFromBottom(R.id.wc2SessionFragment)
        }
        wc2MainViewModel.openWalletConnectRequestLiveEvent.observe(this) { requestId ->
            navHost.navController.slideFromBottom(
                R.id.wc2RequestFragment,
                WC2RequestFragment.prepareParams(requestId)
            )
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (CoreApp.instance.testMode) {
            val rootView = findViewById<ViewGroup>(android.R.id.content)
            val testLabelTv = TextView(this)
            testLabelTv.text = "Test"
            testLabelTv.setPadding(5, 3, 5, 3)
            testLabelTv.includeFontPadding = false
            testLabelTv.setBackgroundColor(Color.RED)
            testLabelTv.setTextColor(Color.WHITE)
            testLabelTv.textSize = 12f
            val layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
            testLabelTv.layoutParams = layoutParams
            rootView.addView(testLabelTv)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // todo: check if we need to shutdown wallet connect related stuff
    }
}
