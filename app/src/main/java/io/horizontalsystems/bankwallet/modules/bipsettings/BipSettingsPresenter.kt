package io.horizontalsystems.bankwallet.modules.bipsettings

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

class BipSettingsPresenter(
        val view: BipSettingsModule.IView,
        val router: BipSettingsModule.IRouter,
        private val interactor: BipSettingsModule.IInteractor,
        private val coinTypes: List<CoinType>,
        val showDoneButton: Boolean)
    : ViewModel(), BipSettingsModule.IViewDelegate {

    private var derivations = mutableListOf<DerivationSetting>()

    override fun onDone() {
        router.closeWithResultOk()
    }

    override fun onViewLoad() {
        setBtcBip()
        setLtcBip()
    }

    override fun onSelect(derivationSetting: DerivationSetting) {
        if (derivations.firstOrNull{ it.coinType == derivationSetting.coinType }?.derivation != derivationSetting.derivation
                && interactor.getWalletForUpdate(derivationSetting.coinType) != null) {
            val coin = interactor.getCoin(derivationSetting.coinType)
            view.showDerivationChangeAlert(derivationSetting, coin.title)
        } else {
            updateDerivation(derivationSetting)
        }
    }

    override fun proceedWithDerivationChange(derivationSetting: DerivationSetting) {
        updateDerivation(derivationSetting)

        interactor.getWalletForUpdate(derivationSetting.coinType)?.let {
            interactor.reSyncWallet(it)
        }
    }

    private fun updateDerivation(derivationSetting: DerivationSetting) {
        interactor.saveDerivation(derivationSetting)
        when(derivationSetting.coinType){
            CoinType.Bitcoin -> view.setBtcBipSelection(derivationSetting.derivation)
            CoinType.Litecoin -> view.setLtcBipSelection(derivationSetting.derivation)
        }
    }

    private fun setBtcBip() {
        val title = interactor.getCoin(CoinType.Bitcoin).title
        view.setBtcTitle(title)

        val selectedBip = interactor.derivation(CoinType.Bitcoin)
        view.setBtcBipSelection(selectedBip)

        derivations.add(DerivationSetting(CoinType.Bitcoin, selectedBip))
        view.setBtcBipsEnabled(coinTypes.contains(CoinType.Bitcoin))
    }

    private fun setLtcBip() {
        val title = interactor.getCoin(CoinType.Litecoin).title
        view.setLtcTitle(title)

        val selectedBip = interactor.derivation(CoinType.Litecoin)
        view.setLtcBipSelection(selectedBip)

        derivations.add(DerivationSetting(CoinType.Litecoin, selectedBip))
        view.setLtcBipsEnabled(coinTypes.contains(CoinType.Litecoin))
    }

}
