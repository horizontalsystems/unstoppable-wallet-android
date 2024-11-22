package io.horizontalsystems.bankwallet.modules.backupalert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import kotlinx.coroutines.delay

@Composable
fun BackupAlert(navController: NavController) {
    val viewModel = viewModel<BackupAlertViewModel>()

    LifecycleResumeEffect(Unit) {
        viewModel.resume()

        onPauseOrDispose {
            viewModel.pause()
        }
    }

    val account = viewModel.account
    if (account != null) {
        LaunchedEffect(account) {
            delay(300)
            viewModel.onHandled()
            navController.slideFromBottom(R.id.backupRecoveryPhraseDialog, account)
        }
    }
}
