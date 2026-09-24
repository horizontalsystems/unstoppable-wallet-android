package io.horizontalsystems.walletkit.modules.send.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.collectSafely
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SendTab { Standard, Private, CrossPay }

data class SendUiState(
    val wallet: Wallet,
    val tab: SendTab,
    val chainSettings: SendChainSettings?,
    val privateSendSupported: Boolean,
) {
    val isPrivateSend: Boolean
        get() = tab == SendTab.Private
}

/**
 * What the send screen's tabs share: the selected tab and the chain's own settings (coin
 * control), which apply to whichever tab sends. Scoped to [SendV2Page], so the settings and
 * confirmation pages opened over the screen reach the same instance. What is typed on a tab
 * lives in that tab's own [SendFormViewModel].
 */
class SendViewModel(
    val wallet: Wallet,
    private val privateSendManager: PrivateSendManager,
) : ViewModelUiState<SendUiState>() {

    private var tab = SendTab.Standard
    private var privateSendSupported = privateSendManager.isSupported(wallet.token)
    private val _chainSettingsFlow = MutableStateFlow<SendChainSettings?>(null)
    val chainSettingsFlow = _chainSettingsFlow.asStateFlow()

    init {
        observePrivateSendSupport()
    }

    // Support is a lookup over synced provider lists; the sync makes a first open show the
    // Private tab without a reload, and the tab is left as soon as support is withdrawn.
    private fun observePrivateSendSupport() {
        viewModelScope.launch {
            try {
                privateSendManager.sync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // The availability flow still reports what could be synced.
            }
        }
        viewModelScope.launch {
            privateSendManager.availabilityFlow.collectSafely {
                val supported = privateSendManager.isSupported(wallet.token)
                if (supported != privateSendSupported) {
                    privateSendSupported = supported
                    if (!supported && tab == SendTab.Private) {
                        tab = SendTab.Standard
                    }
                    emitState()
                }
            }
        }
    }

    override fun createState() = SendUiState(
        wallet = wallet,
        tab = tab,
        chainSettings = _chainSettingsFlow.value,
        privateSendSupported = privateSendSupported,
    )

    fun onSelectTab(tab: SendTab) {
        if (tab == SendTab.Private && !privateSendSupported) return
        this.tab = tab
        emitState()
    }

    fun onChangeChainSettings(settings: SendChainSettings?) {
        _chainSettingsFlow.value = settings
        emitState()
    }

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SendViewModel(wallet, App.privateSendManager) as T
        }
    }
}
