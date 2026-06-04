package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EnabledWalletCache
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun BackupRequiredAlert(navController: NavController) {
    val viewModel = viewModel<BackupRequiredAlertViewModel>()

    val uiState = viewModel.uiState
    val alertAccount = uiState.alertAccount
    LaunchedEffect(alertAccount) {
        if (alertAccount != null) {
            delay(1000)
            viewModel.onAlertShown()
            navController.slideFromBottom(
                R.id.backupRequiredDialog,
                BackupRequiredDialog.Input(alertAccount)
            )
        }
    }
}

class BackupRequiredAlertViewModel : ViewModelUiState<BackupRequiredAlertViewModel.UiState>() {
    private var currentAccount: Account? = null
    private var alertAccount: Account? = null
    private val enabledWalletsCacheDao = App.appDatabase.enabledWalletsCacheDao()
    private var observeBalanceCacheUpdatesJob: Job? = null

    init {
        viewModelScope.launch {
            App.accountManager.activeAccountStateFlow.collect {
                handleAccountUpdate((it as? ActiveAccountState.ActiveAccount)?.account)
            }
        }
    }

    fun onAlertShown() {
        observeBalanceCacheUpdatesJob?.cancel()

        alertAccount = null
        emitState()
    }

    private fun handleAccountUpdate(account: Account?) {
        observeBalanceCacheUpdatesJob?.cancel()
        this.currentAccount = account
        if (account == null || account.hasAnyBackup) return

        observeBalanceCacheUpdatesJob = viewModelScope.launch {
            enabledWalletsCacheDao.flowByAccountId(account.id).collect {
                handleBalanceUpdates(it)
            }
        }
    }

    private fun handleBalanceUpdates(walletCaches: List<EnabledWalletCache>) {
        val hasNonZeroBalance = walletCaches.any {
            it.balanceData != null && it.balanceData.total > BigDecimal.ZERO
        }

        alertAccount = if (hasNonZeroBalance) currentAccount else null

        emitState()
    }

    override fun createState() = UiState(
        alertAccount = alertAccount
    )

    data class UiState(val alertAccount: Account?)
}