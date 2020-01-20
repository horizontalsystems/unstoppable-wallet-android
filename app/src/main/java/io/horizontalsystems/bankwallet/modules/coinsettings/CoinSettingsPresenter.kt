package io.horizontalsystems.bankwallet.modules.coinsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinSetting
import io.horizontalsystems.bankwallet.entities.SyncMode

class CoinSettingsPresenter(
        val view: CoinSettingsModule.IView,
        val router: CoinSettingsModule.IRouter,
        private val mode: SettingsMode,
        private val interactor: CoinSettingsModule.IInteractor)
    : ViewModel(), CoinSettingsModule.IViewDelegate {

    private var selectedDerivation = interactor.bitcoinDerivation()
    private var selectedSyncMode = interactor.syncMode()
    var showDoneButton = mode == SettingsMode.InsideRestore

    override fun onLoad() {
        updatedView()
    }

    override fun onSelect(syncMode: SyncMode) {
        if (selectedSyncMode != syncMode && interactor.getWalletsForSyncModeUpdate().isNotEmpty()) {
            view.showSyncModeChangeAlert(syncMode)
        } else {
            interactor.updateSyncMode(syncMode)
            updatedView()

        }
    }

    override fun onSelect(derivation: AccountType.Derivation) {
        if (selectedDerivation != derivation && interactor.getWalletsForDerivationUpdate().isNotEmpty()) {
            view.showDerivationChangeAlert(derivation)
        } else {
            interactor.updateBitcoinDerivation(derivation)
            updatedView()
        }
    }

    override fun proceedWithDerivationChange(derivation: AccountType.Derivation) {
        selectedDerivation = derivation
        interactor.updateBitcoinDerivation(selectedDerivation)
        updatedView()

        //update derivation
        val wallets = interactor.getWalletsForDerivationUpdate()
        wallets.forEach { wallet ->
            wallet.settings[CoinSetting.Derivation] = derivation.value
        }
        interactor.reSyncWalletsWithNewSettings(wallets)
    }

    override fun proceedWithSyncModeChange(syncMode: SyncMode) {
        selectedSyncMode = syncMode
        interactor.updateSyncMode(syncMode)
        updatedView()

        //update syncMode
        val wallets = interactor.getWalletsForSyncModeUpdate()
        wallets.forEach { wallet ->
            wallet.settings[CoinSetting.SyncMode] = syncMode.value
        }
        interactor.reSyncWalletsWithNewSettings(wallets)
    }

    override fun onDone() {
        when (mode) {
            SettingsMode.InsideRestore -> router.closeWithResultOk()
            SettingsMode.StandAlone -> router.close()
        }
    }

    private fun updatedView() {
        val derivation = interactor.bitcoinDerivation()
        val restoreSource = interactor.syncMode()
        view.setSelection(derivation, restoreSource)
    }

}
