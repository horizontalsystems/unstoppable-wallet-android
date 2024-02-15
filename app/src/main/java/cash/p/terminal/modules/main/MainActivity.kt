package cash.p.terminal.modules.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import cash.p.terminal.R
import cash.p.terminal.core.BaseActivity
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.walletconnect.AuthEvent
import cash.p.terminal.modules.walletconnect.SignEvent
import cash.p.terminal.modules.walletconnect.WCViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val wcViewModel = WCViewModel()

        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener(this)
        handleWeb3WalletEvents(navController, wcViewModel)

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }
    }

    private fun handleWeb3WalletEvents(
        navController: NavController,
        wcViewModel: WCViewModel,
    ) {
        wcViewModel.walletEvents
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { event ->
                when (event) {
                    is SignEvent.SessionProposal -> navController.slideFromBottom(R.id.wcSessionFragment)
                    is SignEvent.SessionRequest -> {
                        navController.slideFromBottom(R.id.wcRequestFragment,)
                    }

                    is SignEvent.Disconnect -> {
                    }

                    is AuthEvent.OnRequest -> {
                    }

                    else -> Unit
                }
            }
            .launchIn(lifecycleScope)
    }
}
