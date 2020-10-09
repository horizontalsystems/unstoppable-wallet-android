package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainViewModel

class WalletConnectViewModel(
        val service: WalletConnectService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val initialScreen: InitialScreen
        get() = when {
            !service.isEthereumKitReady -> {
                InitialScreen.NoEthereumKit
            }
            service.state == WalletConnectService.State.Idle -> {
                InitialScreen.ScanQrCode
            }
            else -> {
                InitialScreen.Main
            }
        }

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    enum class InitialScreen {
        NoEthereumKit, ScanQrCode, Main
    }
}