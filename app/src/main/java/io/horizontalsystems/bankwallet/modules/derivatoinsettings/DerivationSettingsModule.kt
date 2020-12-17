package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.DerivationSetting

object DerivationSettingsModule {

    interface IView {
        fun showDerivationChangeAlert(derivationSetting: DerivationSetting, coinTitle: String)
        fun setViewItems(viewItems: List<DerivationSettingSectionViewItem>)
    }

    interface IViewDelegate {
        fun onSelect(sectionIndex: Int, settingIndex: Int)
        fun onViewLoad()
        fun onConfirm(derivationSetting: DerivationSetting)
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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = DerivationSettingsView()
            val interactor = DerivationSettingsInteractor(App.derivationSettingsManager, App.coinManager)
            val presenter = DerivationSettingsPresenter(view, interactor, StringProvider(App.instance))

            return presenter as T
        }
    }
}

data class DerivationSettingsItem (val coin: Coin, val setting: DerivationSetting)

data class DerivationSettingViewItem (val title: String, val subtitle: String, val selected: Boolean)

data class DerivationSettingSectionViewItem (val coinName: String, val items: List<DerivationSettingViewItem>)
