package io.horizontalsystems.bankwallet.modules.backupalert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import kotlinx.coroutines.delay

@Composable
fun BackupAlert(navController: NavController) {
    val viewModel = viewModel<BackupAlertViewModel>()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.resume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.pause()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val account = viewModel.account
    if (account != null) {
        LaunchedEffect(account) {
            delay(300)
            viewModel.onHandled()
            navController.slideFromBottom(R.id.backupRecoveryPhraseDialog, BackupRecoveryPhraseDialog.prepareParams(account))
        }
    }
}
