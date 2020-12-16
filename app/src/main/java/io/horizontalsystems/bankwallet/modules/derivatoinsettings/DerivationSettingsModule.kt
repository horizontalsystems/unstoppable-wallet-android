package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

object DerivationSettingsModule {

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
        val allActiveSettings: List<Pair<DerivationSetting, Coin>>
        fun derivation(coinType: CoinType): Derivation
        fun getCoin(coinType: CoinType): Coin
        fun saveDerivation(derivationSetting: DerivationSetting)
    }

    interface IRouter {
        fun close()
    }

    class Factory(private val coinTypes: List<CoinType>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = DerivationSettingsView()
            val router = DerivationSettingsRouter()
            val interactor = DerivationSettingsInteractor(App.derivationSettingsManager, App.coinManager)
            val presenter = DerivationSettingsPresenter(view, router, interactor, coinTypes)

            return presenter as T
        }
    }
}
