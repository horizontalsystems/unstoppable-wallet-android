package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BackupRequiredAlert(navController: HSNavigation) {
    val viewModel = viewModel<BackupRequiredAlertViewModel>()

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
            navController.slideFromBottom(
                BackupRequiredSheet(BackupRequiredSheet.Input(account))
            )
        }
    }
}

class BackupRequiredAlertViewModel : ViewModel() {
    var account by mutableStateOf<Account?>(null)
        private set

    private var job: Job? = null

    fun resume() {
        job = viewModelScope.launch {
            App.accountManager.newAccountBackupRequiredFlow.collect {
                account = it
            }
        }
    }

    fun pause() {
        job?.cancel()
    }

    fun onHandled() {
        App.accountManager.onHandledBackupRequiredNewAccount()
    }

}