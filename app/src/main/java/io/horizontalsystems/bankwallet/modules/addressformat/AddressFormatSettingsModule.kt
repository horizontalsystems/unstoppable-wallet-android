package io.horizontalsystems.bankwallet.modules.addressformat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting
import io.horizontalsystems.bankwallet.entities.Wallet

object AddressFormatSettingsModule {

    interface IView {
        fun setBtcBipSelection(selectedBip: Derivation)
        fun setLtcBipSelection(selectedBip: Derivation)
        fun setBtcTitle(title: String)
        fun setLtcTitle(title: String)
        fun showDerivationChangeAlert(derivationSetting: DerivationSetting, coinTitle: String)
        fun setLtcBipVisibility(isVisible: Boolean)
        fun setBtcBipVisibility(isVisible: Boolean)
    }

    interface IViewDelegate {
        fun onSelect(derivationSetting: DerivationSetting)
        fun onViewLoad()
        fun proceedWithDerivationChange(derivationSetting: DerivationSetting)
    }

    interface IInteractor {
        fun derivation(coinType: CoinType): Derivation
        fun getCoin(coinType: CoinType): Coin
        fun getWalletForUpdate(coinType: CoinType): Wallet?
        fun saveDerivation(derivationSetting: DerivationSetting)
        fun reSyncWallet(wallet: Wallet)
    }

    interface IRouter {
        fun close()
    }

    class Factory(private val coinTypes: List<CoinType>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = AddressFormatSettingsView()
            val router = AddressFormatSettingsRouter()
            val interactor = AddressFormatSettingsInteractor(App.derivationSettingsManager, App.coinManager, App.walletManager, App.adapterManager)
            val presenter = AddressFormatSettingsPresenter(view, router, interactor, coinTypes)

            return presenter as T
        }
    }
}
