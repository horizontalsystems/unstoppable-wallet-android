package cash.p.terminal.core.notifications

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.modules.premium.settings.PollingInterval
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.wallet.IWalletManager
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class TransactionNotificationCoordinator(
    private val application: android.app.Application,
    private val localStorage: ILocalStorage,
    private val notificationManager: TransactionNotificationManager,
    private val backgroundManager: BackgroundManager,
    private val checkPremiumUseCase: CheckPremiumUseCase,
    private val keepAliveManager: BackgroundKeepAliveManager,
    private val walletManager: IWalletManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var serviceRunning = false

    fun start() {
        backgroundManager.onBeforeEnterBackground = {
            if (shouldStartService()) {
                startService()
            }
        }

        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground && serviceRunning) {
                    stopService()
                }
            }
        }
    }

    private fun shouldStartService(): Boolean {
        if (!checkPremiumUseCase.getPremiumType().isPremium()) return false
        if (!localStorage.pushNotificationsEnabled) return false
        if (localStorage.pushEnabledBlockchainUids.isEmpty()) return false
        if (!notificationManager.hasNotificationPermission()) return false
        if (!notificationManager.isTransactionChannelEnabled()) return false
        if (!notificationManager.isServiceChannelEnabled()) return false
        return true
    }

    private fun startService() {
        val enabledUids = localStorage.pushEnabledBlockchainUids
        val monitoredTypes = walletManager.activeWallets
            .map { it.token.blockchainType }
            .filter { it.uid in enabledUids }
            .toSet()

        if (localStorage.pushPollingInterval == PollingInterval.REALTIME) {
            keepAliveManager.setKeepAlive(monitoredTypes)
        } else {
            // In polling mode no keep-alive is required: each kit is brought up
            // only for the duration of its own poll cycle via
            // startForPolling/stopForPolling.
            keepAliveManager.setKeepAlive(emptySet())
        }

        Timber.d("Starting transaction notification service")
        serviceRunning = TransactionNotificationService.start(application)
    }

    private fun stopService() {
        Timber.d("Stopping transaction notification service")
        TransactionNotificationService.stop(application)
        keepAliveManager.clear()
        serviceRunning = false
    }
}
