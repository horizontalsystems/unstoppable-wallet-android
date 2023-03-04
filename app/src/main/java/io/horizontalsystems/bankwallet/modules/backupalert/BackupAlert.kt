package io.horizontalsystems.bankwallet.modules.backupalert

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.ui.compose.DisposableLifecycleCallbacks
import kotlinx.coroutines.delay

@Composable
fun BackupAlert(navController: NavController) {
    val viewModel = viewModel<BackupAlertViewModel>()

    DisposableLifecycleCallbacks(
        onResume = viewModel::resume,
        onPause = viewModel::pause
    )

    val account = viewModel.account
    if (account != null) {
        LaunchedEffect(account) {
            delay(300)
            viewModel.onHandled()
            navController.slideFromBottom(R.id.backupRecoveryPhraseDialog, BackupRecoveryPhraseDialog.prepareParams(account))
        }
    }
}
