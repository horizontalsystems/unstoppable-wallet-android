package cash.p.terminal.modules.premium.settings

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.modules.blockchainsettings.SupportedBlockchainsFactory
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.ViewModelUiState
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.imageUrl
import kotlinx.coroutines.launch
import timber.log.Timber

internal class PushNotificationsViewModel(
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,
    private val marketKit: MarketKitWrapper,
    private val dispatcherProvider: DispatcherProvider,
    private val localStorage: ILocalStorage,
) : ViewModelUiState<PushNotificationsUiState>() {

    private var showNotifications = localStorage.pushNotificationsEnabled
    private var pollingInterval = localStorage.pushPollingInterval
    private var showBlockchainName = localStorage.pushShowBlockchainName
    private var showCoinAmount = localStorage.pushShowCoinAmount
    private var showFiatAmount = localStorage.pushShowFiatAmount
    private var loading = true
    private var blockchainViewItems = emptyList<PushNotificationBlockchainViewItem>()

    private var enabledUids = localStorage.pushEnabledBlockchainUids

    init {
        loadBlockchains()
    }

    override fun createState() = PushNotificationsUiState(
        showNotifications = showNotifications,
        pollingInterval = pollingInterval,
        showBlockchainName = showBlockchainName,
        showCoinAmount = showCoinAmount,
        showFiatAmount = showFiatAmount,
        blockchains = blockchainViewItems,
        loading = loading,
    )

    fun setShowNotifications(enabled: Boolean) {
        showNotifications = enabled
        localStorage.pushNotificationsEnabled = enabled
        emitState()
    }

    fun setPollingInterval(interval: PollingInterval) {
        pollingInterval = interval
        localStorage.pushPollingInterval = interval
        emitState()
    }

    fun setShowBlockchainName(enabled: Boolean) {
        showBlockchainName = enabled
        localStorage.pushShowBlockchainName = enabled
        emitState()
    }

    fun setShowCoinAmount(enabled: Boolean) {
        showCoinAmount = enabled
        localStorage.pushShowCoinAmount = enabled
        emitState()
    }

    fun setShowFiatAmount(enabled: Boolean) {
        showFiatAmount = enabled
        localStorage.pushShowFiatAmount = enabled
        emitState()
    }

    fun setBlockchainNotifications(blockchainUid: String, enabled: Boolean) {
        enabledUids = if (enabled) {
            enabledUids + blockchainUid
        } else {
            enabledUids - blockchainUid
        }
        localStorage.pushEnabledBlockchainUids = enabledUids
        localStorage.pushBlockchainsConfigured = true

        blockchainViewItems = blockchainViewItems.map { item ->
            if (item.uid == blockchainUid) {
                item.copy(notificationsEnabled = enabled)
            } else {
                item
            }
        }
        emitState()
    }

    private fun loadBlockchains() {
        viewModelScope.launch(dispatcherProvider.io) {
            val loadedBlockchains = try {
                SupportedBlockchainsFactory.create(
                    btcBlockchainManager = btcBlockchainManager,
                    evmBlockchainManager = evmBlockchainManager,
                    solanaRpcSourceManager = solanaRpcSourceManager,
                    marketKit = marketKit,
                ).all
            } catch (t: Throwable) {
                Timber.e(t, "Failed to load push notification blockchains")
                emptyList()
            }

            if (!localStorage.pushBlockchainsConfigured && loadedBlockchains.isNotEmpty()) {
                enabledUids = loadedBlockchains.map { it.uid }.toSet()
                localStorage.pushEnabledBlockchainUids = enabledUids
                localStorage.pushBlockchainsConfigured = true
            }

            blockchainViewItems = loadedBlockchains.map { it.toViewItem() }
            loading = false
            emitState()
        }
    }

    private fun Blockchain.toViewItem() = PushNotificationBlockchainViewItem(
        uid = uid,
        name = name,
        imageUrl = type.imageUrl,
        notificationsEnabled = uid in enabledUids,
    )
}

internal data class PushNotificationsUiState(
    val showNotifications: Boolean,
    val pollingInterval: PollingInterval,
    val showBlockchainName: Boolean,
    val showCoinAmount: Boolean,
    val showFiatAmount: Boolean,
    val blockchains: List<PushNotificationBlockchainViewItem>,
    val loading: Boolean,
)

internal data class PushNotificationBlockchainViewItem(
    val uid: String,
    val name: String,
    val imageUrl: String,
    val notificationsEnabled: Boolean,
)
