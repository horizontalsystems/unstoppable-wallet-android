package io.horizontalsystems.walletkit.modules.settings.main

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.IPinComponent
import io.horizontalsystems.walletkit.ISystemInfoManager
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.IBackupManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.core.providers.IAppConfigProvider
import io.horizontalsystems.walletkit.modules.walletconnect.WCManager
import io.horizontalsystems.walletkit.modules.walletconnect.WCSessionManager
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class MainSettingsViewModel(
    private val backupManager: IBackupManager,
    private val systemInfoManager: ISystemInfoManager,
    private val pinComponent: IPinComponent,
    private val wcSessionManager: WCSessionManager,
    private val wcManager: WCManager,
    private val accountManager: IAccountManager,
    private val appConfigProvider: IAppConfigProvider,
) : ViewModelUiState<MainSettingUiState>() {

    val fdroidSupportLink by lazy {
        appConfigProvider.simplexSupportChat
    }
    val vipSupportLink by lazy {
        appConfigProvider.telegramSupportChat
    }

    val appVersion: String
        get() {
            var appVersion = systemInfoManager.appVersion
            if (io.horizontalsystems.walletkit.BuildConfig.DEBUG) {
                appVersion += " (${appConfigProvider.appBuild})"
            }

            return appVersion
        }

    val walletConnectSupportState: WCManager.SupportState
        get() = wcManager.getWalletConnectSupportState()

    // Donating means sending, so a watch account is not offered it. Only a known watch
    // account hides the cell; while accounts are still loading there is nothing to hide.
    private val donateEnabled: Boolean
        get() = accountManager.activeAccount?.isWatchAccount != true

    private val appWebPageLink = appConfigProvider.appWebPageLink
    private val hasNonStandardAccount: Boolean
        get() = accountManager.hasNonStandardAccount

    private val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    private val isPinSet: Boolean
        get() = pinComponent.isPinSet


    private var wcPendingRequestCount = 0
    private var hasSubscription = false

    init {
        viewModelScope.launch {
            backupManager.allBackedUpFlow.collectSafely {
                emitState()
            }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlow.collect {
                emitState()
            }
        }

        viewModelScope.launch {
            accountManager.activeAccountStateFlow.collect {
                emitState()
            }
        }

        viewModelScope.launch {
            wcSessionManager.pendingRequestCountFlow.collect {
                wcPendingRequestCount = it
                emitState()
            }
        }

        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                hasSubscription = it != null
                emitState()
            }
        }
    }

    override fun createState(): MainSettingUiState {
        return MainSettingUiState(
            appWebPageLink = appWebPageLink,
            hasNonStandardAccount = hasNonStandardAccount,
            allBackedUp = allBackedUp,
            pendingRequestCount = wcPendingRequestCount,
            manageWalletShowAlert = !allBackedUp || hasNonStandardAccount,
            securityCenterShowAlert = !isPinSet,
            hasSubscription = hasSubscription,
            donateEnabled = donateEnabled,
        )
    }
}

data class MainSettingUiState(
    val appWebPageLink: String,
    val hasNonStandardAccount: Boolean,
    val allBackedUp: Boolean,
    val pendingRequestCount: Int,
    val manageWalletShowAlert: Boolean,
    val securityCenterShowAlert: Boolean,
    val hasSubscription: Boolean,
    val donateEnabled: Boolean,
)