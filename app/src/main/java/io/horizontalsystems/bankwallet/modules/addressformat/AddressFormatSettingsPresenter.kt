package io.horizontalsystems.bankwallet.modules.addressformat

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

class AddressFormatSettingsPresenter(
        val view: AddressFormatSettingsModule.IView,
        val router: AddressFormatSettingsModule.IRouter,
        private val interactor: AddressFormatSettingsModule.IInteractor,
        private val coinTypes: List<CoinType>,
        val showDoneButton: Boolean)
    : ViewModel(), AddressFormatSettingsModule.IViewDelegate {

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
        if(coinTypes.contains(CoinType.Bitcoin)){
            val title = interactor.getCoin(CoinType.Bitcoin).title
            view.setBtcTitle(title)

            val selectedBip = interactor.derivation(CoinType.Bitcoin)
            view.setBtcBipSelection(selectedBip)

            derivations.add(DerivationSetting(CoinType.Bitcoin, selectedBip))
        }

        view.setBtcBipVisibility(coinTypes.contains(CoinType.Bitcoin))
    }

    private fun setLtcBip() {
        if(coinTypes.contains(CoinType.Litecoin)){
            val title = interactor.getCoin(CoinType.Litecoin).title
            view.setLtcTitle(title)

            val selectedBip = interactor.derivation(CoinType.Litecoin)
            view.setLtcBipSelection(selectedBip)

            derivations.add(DerivationSetting(CoinType.Litecoin, selectedBip))
        }

        view.setLtcBipVisibility(coinTypes.contains(CoinType.Litecoin))
    }

}
