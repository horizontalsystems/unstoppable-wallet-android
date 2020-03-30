package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.SyncMode

class BlockchainSettingsPresenter(
        val view: BlockchainSettingsModule.IView,
        val router: BlockchainSettingsModule.IRouter,
        private val interactor: BlockchainSettingsModule.IInteractor,
        private val coinType: CoinType)
    : ViewModel(), BlockchainSettingsModule.IViewDelegate {

    private var derivation = interactor.derivation(coinType)
    private var syncMode = interactor.syncMode(coinType)
    private val coinTitle: String = interactor.coinWithSetting(coinType)?.title ?: ""

    override fun onViewLoad() {
        view.setTitle(coinTitle)
        setSettings()
    }

    private fun setSettings() {
        derivation?.let {
            view.setDerivation(it)
        }
        syncMode?.let {
            val syncMode = if (it == SyncMode.New) SyncMode.Fast else it
            view.setSyncMode(syncMode)
        }
        view.setSourceLink(coinType)
    }

    private fun updateSyncMode(syncMode: SyncMode) {
        this.syncMode = syncMode
        interactor.saveSyncMode(coinType, syncMode)
        view.setSyncMode(syncMode)
    }

    private fun updateDerivation(derivation: Derivation) {
        this.derivation = derivation
        interactor.saveDerivation(coinType, derivation)
        view.setDerivation(derivation)
    }

    override fun onSelect(syncMode: SyncMode) {
        if (this.syncMode != syncMode && interactor.getWalletForUpdate(coinType) != null) {
            view.showSyncModeChangeAlert(syncMode, coinTitle)
        } else {
            updateSyncMode(syncMode)
        }
    }

    override fun onSelect(derivation: Derivation) {
        if (this.derivation != derivation && interactor.getWalletForUpdate(coinType) != null) {
            view.showDerivationChangeAlert(derivation, coinTitle)
        } else {
            updateDerivation(derivation)
        }
    }

    override fun proceedWithDerivationChange(derivation: Derivation) {
        updateDerivation(derivation)

        interactor.getWalletForUpdate(coinType)?.let {
            interactor.reSyncWallet(it)
        }
    }

    override fun proceedWithSyncModeChange(syncMode: SyncMode) {
        updateSyncMode(syncMode)

        interactor.getWalletForUpdate(coinType)?.let {
            interactor.reSyncWallet(it)
        }
    }

}
