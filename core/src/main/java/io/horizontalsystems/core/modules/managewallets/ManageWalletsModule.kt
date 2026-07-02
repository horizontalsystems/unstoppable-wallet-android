package io.horizontalsystems.core.modules.managewallets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.modules.balance.BalanceService
import io.horizontalsystems.core.modules.enablecoin.restoresettings.RestoreSettingsService
import io.horizontalsystems.core.modules.enablecoin.restoresettings.RestoreSettingsViewModel
import io.horizontalsystems.core.modules.receive.FullCoinsProvider

object ManageWalletsModule {

    class Factory : ViewModelProvider.Factory {

        private val restoreSettingsService by lazy {
            RestoreSettingsService(App.restoreSettingsManager, App.zcashBirthdayProvider, App.moneroBirthdayProvider)
        }

        private val manageWalletsService by lazy {
            val activeAccount = App.accountManager.activeAccount
            ManageWalletsService(
                App.walletManager,
                restoreSettingsService,
                App.accountManager.activeAccount?.let { account ->
                    FullCoinsProvider(App.marketKit, account)
                },
                activeAccount,
                BalanceService.getInstance("wallet"),
            )
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                RestoreSettingsViewModel::class.java -> {
                    RestoreSettingsViewModel(restoreSettingsService, listOf(restoreSettingsService)) as T
                }
                ManageWalletsViewModel::class.java -> {
                    ManageWalletsViewModel(manageWalletsService, listOf(manageWalletsService)) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }
}
