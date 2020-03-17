package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsPresenter(
        val view: BlockchainSettingsModule.IView,
        val router: BlockchainSettingsModule.IRouter,
        private val interactor: BlockchainSettingsModule.IInteractor,
        private val coinType: CoinType)
    : ViewModel(), BlockchainSettingsModule.IViewDelegate {

    private var blockchainSettings = interactor.blockchainSettings(coinType)
    private val coinTitle: String = interactor.coinWithSetting(coinType)?.title ?: ""

    override fun onViewLoad() {
        view.setTitle(coinTitle)
        setSettings()
    }

    private fun setSettings() {
        blockchainSettings?.derivation?.let {
            view.setDerivation(it)
        }
        blockchainSettings?.syncMode?.let {
            val syncMode = if (it == SyncMode.New) SyncMode.Fast else it
            view.setSyncMode(syncMode)
        }
        view.setSourceLink(coinType)
    }

    override fun onSelect(syncMode: SyncMode) {
        val blockchainSettings = blockchainSettings ?: return

        if (blockchainSettings.syncMode != syncMode && interactor.getWalletForUpdate(coinType) != null) {
            view.showSyncModeChangeAlert(syncMode, coinTitle)
        } else {
            blockchainSettings.syncMode = syncMode
            interactor.updateSettings(blockchainSettings)
            view.setSyncMode(syncMode)
        }
    }

    override fun onSelect(derivation: AccountType.Derivation) {
        val blockchainSettings = blockchainSettings ?: return

        if (blockchainSettings.derivation != derivation && interactor.getWalletForUpdate(coinType) != null) {
            view.showDerivationChangeAlert(derivation, coinTitle)
        } else {
            blockchainSettings.derivation = derivation
            interactor.updateSettings(blockchainSettings)
            view.setDerivation(derivation)
        }
    }

    override fun proceedWithDerivationChange(derivation: AccountType.Derivation) {
        val blockchainSettings = blockchainSettings ?: return

        blockchainSettings.derivation = derivation
        interactor.updateSettings(blockchainSettings)

        view.setDerivation(derivation)

        interactor.getWalletForUpdate(coinType)?.let {
            interactor.reSyncWallet(it)
        }
    }

    override fun proceedWithSyncModeChange(syncMode: SyncMode) {
        val blockchainSettings = blockchainSettings ?: return

        blockchainSettings.syncMode = syncMode
        interactor.updateSettings(blockchainSettings)

        view.setSyncMode(syncMode)

        interactor.getWalletForUpdate(coinType)?.let {
            interactor.reSyncWallet(it)
        }
    }

}
