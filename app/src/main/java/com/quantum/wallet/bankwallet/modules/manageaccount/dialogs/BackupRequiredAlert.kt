package com.quantum.wallet.bankwallet.modules.manageaccount.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.slideFromBottom
import com.quantum.wallet.bankwallet.entities.Account
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BackupRequiredAlert(navController: NavController) {
    val viewModel = viewModel<BackupRequiredAlertViewModel>()

    LifecycleResumeEffect(Unit) {
        viewModel.resume()

        onPauseOrDispose {
            viewModel.pause()
        }
    }

    val account = viewModel.account
    if (account != null) {
        val text = Translator.getString(
            R.string.Balance_Receive_BackupRequired_Description,
            account.name,
        )
        LaunchedEffect(account) {
            delay(300)
            viewModel.onHandled()
            navController.slideFromBottom(
                R.id.backupRequiredDialog,
                BackupRequiredDialog.Input(account, text)
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