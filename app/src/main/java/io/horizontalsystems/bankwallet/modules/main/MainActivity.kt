package io.horizontalsystems.bankwallet.modules.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.core.hideKeyboard

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    override fun onResume() {
        super.onResume()
        validate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val navHost =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHost.navController

        navController.setGraph(R.navigation.main_graph, intent.extras)
        navController.addOnDestinationChangedListener { _, _, _ ->
            currentFocus?.hideKeyboard(this)
        }

        viewModel.navigateToMainLiveData.observe(this) {
            if (it) {
                navController.popBackStack(navController.graph.startDestinationId, false)
                viewModel.onNavigatedToMain()
            }
        }

        viewModel.wcEvent.observe(this) { wcEvent ->
            if (wcEvent != null) {
                when (wcEvent) {
                    is Wallet.Model.SessionRequest -> {
                        navController.slideFromBottom(R.id.wcRequestFragment)
                    }
                    is Wallet.Model.SessionProposal -> {
                        navController.slideFromBottom(R.id.wcSessionFragment)
                    }
                    else -> {}
                }

                viewModel.onWcEventHandled()
            }
        }
    }

    private fun validate() = try {
        viewModel.validate()
    } catch (e: MainScreenValidationError.NoSystemLock) {
        KeyStoreActivity.startForNoSystemLock(this)
        finish()
    } catch (e: MainScreenValidationError.KeyInvalidated) {
        KeyStoreActivity.startForInvalidKey(this)
        finish()
    } catch (e: MainScreenValidationError.UserAuthentication) {
        KeyStoreActivity.startForUserAuthentication(this)
        finish()
    } catch (e: MainScreenValidationError.Welcome) {
        IntroActivity.start(this)
        finish()
    } catch (e: MainScreenValidationError.Unlock) {
        LockScreenActivity.start(this)
    }
}
