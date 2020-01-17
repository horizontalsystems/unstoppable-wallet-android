package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsPresenter(
        private val mode: SettingsMode,
        val view: CoinSettingsModule.IView,
        val router: CoinSettingsModule.IRouter,
        private val interactor: CoinSettingsModule.IInteractor)
    : ViewModel(), CoinSettingsModule.IViewDelegate {

    override fun onLoad() {
        updatedView()
    }

    override fun onSelect(syncMode: SyncMode) {
        interactor.updateSyncMode(syncMode)
        updatedView()
    }

    override fun onSelect(derivation: AccountType.Derivation) {
        interactor.updateBitcoinDerivation(derivation)
        updatedView()
    }

    override fun onDone() {
        when (mode) {
            SettingsMode.Restore -> router.closeWithResultOk()
            SettingsMode.Settings -> router.close()
        }
    }

    private fun updatedView() {
        val derivation = interactor.bitcoinDerivation()
        val restoreSource = interactor.syncMode()
        view.setSelection(derivation, restoreSource)
    }

}
