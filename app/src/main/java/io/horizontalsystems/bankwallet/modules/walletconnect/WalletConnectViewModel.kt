package io.horizontalsystems.bankwallet.modules.walletconnect

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.modules.walletconnect.main.WalletConnectMainPresenter
import io.horizontalsystems.bankwallet.modules.walletconnect.scanqr.WalletConnectScanQrPresenter

class WalletConnectViewModel(
        val service: WalletConnectService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val mainPresenter by lazy {
        WalletConnectMainPresenter(service)
    }

    val scanQrPresenter by lazy {
        WalletConnectScanQrPresenter(service)
    }

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