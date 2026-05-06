package io.horizontalsystems.bankwallet.modules.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.modules.intro.IntroActivity
import io.horizontalsystems.bankwallet.modules.keystore.KeyStoreActivity
import io.horizontalsystems.bankwallet.modules.nav3.Nav3
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class MainActivity : BaseActivity() {

    private val viewModel by viewModels<MainActivityViewModel> {
        MainActivityViewModel.Factory()
    }

    override fun onResume() {
        super.onResume()
        validate()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.setIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                Nav3(viewModel)
            }
        }

//        TODO("xxx nav3")
//        val navHost =
//            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
//        val navController = navHost.navController
//
//        navController.setGraph(R.navigation.main_graph, intent.extras)
//        navController.addOnDestinationChangedListener { _, _, _ ->
//            currentFocus?.hideKeyboard(this)
//        }
//
//        viewModel.wcEvent.observe(this) { wcEvent ->
//            if (wcEvent != null) {
//                when (wcEvent) {
//                    is HSDAppEvent.SessionRequest -> {
//                        navController.slideFromBottom(R.id.wcRequestFragment)
//                    }
//
//                    is HSDAppEvent.SessionProposal -> {
//                        navController.slideFromBottom(R.id.wcSessionBottomSheetDialog)
//                    }
//
//                    is HSDAppEvent.Error -> {
//                        navHost.view?.let {
//                            HudHelper.showErrorMessage(it, wcEvent.throwable.message ?: "Error")
//                        }
//                    }
//
//                    is HSDAppEvent.SessionSettled -> {
//                        navHost.view?.let {
//                            HudHelper.showSuccessMessage(it, getString(R.string.Hud_Text_Connected))
//                        }
//                    }
//
//                    else -> {}
//                }
//
//                viewModel.onWcEventHandled()
//            }
//        }

        viewModel.tcSendRequest.observe(this) { request ->
            if (request != null) {
//                TODO("xxx nav3")
//                navController.slideFromBottom(R.id.tcSendRequestFragment)
            }
        }

        viewModel.tcDappRequest.observe(this) { request ->
            if (request != null) {
//                TODO("xxx nav3")
//                navController.slideFromBottomForResult<TonConnectNewFragment.Result>(
//                    R.id.tcNewFragment,
//                    request.dAppRequest
//                ) { result ->
//                    if (request.closeAppOnResult) {
//                        if (result.approved) {
//                            //Need delay to get connected before closing activity
//                            closeAfterDelay()
//                        } else {
//                            finish()
//                        }
//                    }
//                }
//                viewModel.onTcDappRequestHandled()
            }
        }

        viewModel.setIntent(intent)
    }

    private fun closeAfterDelay() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        handler.postDelayed({
            finish()
        }, 1000)
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
    } catch (e: MainScreenValidationError.KeystoreRuntimeException) {
        Toast.makeText(App.instance, "Issue with Keystore", Toast.LENGTH_SHORT).show()
        finish()
    }
}
