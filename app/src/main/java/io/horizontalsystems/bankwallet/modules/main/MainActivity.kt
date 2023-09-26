package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WC2RequestFragment
import io.horizontalsystems.bankwallet.modules.walletconnect.session.v2.WC2MainViewModel

class MainActivity : BaseActivity() {

    private val wc2MainViewModel by viewModels<WC2MainViewModel> {
        WC2MainViewModel.Factory()
    }

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener(this)

        wc2MainViewModel.sessionProposalLiveEvent.observe(this) {
            navController.slideFromBottom(R.id.wc2SessionFragment)
        }
        wc2MainViewModel.openWalletConnectRequestLiveEvent.observe(this) { requestId ->
            navController.slideFromBottom(
                R.id.wc2RequestFragment,
                WC2RequestFragment.prepareParams(requestId)
            )
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // todo: check if we need to shutdown wallet connect related stuff
    }
}
