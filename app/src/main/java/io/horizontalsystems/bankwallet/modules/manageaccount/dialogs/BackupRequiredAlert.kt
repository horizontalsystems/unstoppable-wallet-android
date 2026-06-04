package io.horizontalsystems.bankwallet.modules.manageaccount.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EnabledWalletCache
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

@Composable
fun BackupRequiredAlert(navController: HSNavigation) {
    val viewModel = hiltViewModel<BackupRequiredAlertViewModel>()

    val uiState = viewModel.uiState
    val alertAccount = uiState.alertAccount
    LaunchedEffect(alertAccount, uiState.isLocked) {
        if (alertAccount != null && !uiState.isLocked) {
            delay(1000)
            viewModel.onAlertShown()
            navController.slideFromBottom(
                BackupRequiredSheet(BackupRequiredSheet.Input(alertAccount))
            )
        }
    }
}

@HiltViewModel
class BackupRequiredAlertViewModel @javax.inject.Inject constructor() : ViewModelUiState<BackupRequiredAlertViewModel.UiState>() {
    private var currentAccount: Account? = null
    private var alertAccount: Account? = null
    private val enabledWalletsCacheDao = App.appDatabase.enabledWalletsCacheDao()
    private var observeBalanceCacheUpdatesJob: Job? = null
    private var isLocked = App.pinComponent.isLockedFlow.value

    init {
        viewModelScope.launch {
            App.accountManager.activeAccountStateFlow.collect {
                handleAccountUpdate((it as? ActiveAccountState.ActiveAccount)?.account)
            }
        }
        viewModelScope.launch {
            App.pinComponent.isLockedFlow.collect {
                isLocked = it
                emitState()
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
        alertAccount = alertAccount,
        isLocked = isLocked
    )

    data class UiState(val alertAccount: Account?, val isLocked: Boolean)
}