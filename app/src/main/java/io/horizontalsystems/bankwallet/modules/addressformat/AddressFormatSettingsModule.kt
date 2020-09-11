package io.horizontalsystems.bankwallet.modules.addressformat

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
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
        fun onDone()
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
        fun closeWithResultOk()
        fun close()
    }

    fun startForResult(context: Context, coinTypes: List<CoinType>, showDoneButton: Boolean) {
        val intent = Intent(context, AddressFormatSettingsActivity::class.java)
        intent.putParcelableArrayListExtra(ModuleField.COIN_TYPES, ArrayList(coinTypes))
        intent.putExtra(ModuleField.SHOW_DONE_BUTTON, showDoneButton)
        context.startActivity(intent)
    }

    class Factory(private val coinTypes: List<CoinType>, private val showDoneButton: Boolean) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = AddressFormatSettingsView()
            val router = AddressFormatSettingsRouter()
            val interactor = AddressFormatSettingsInteractor(App.derivationSettingsManager, App.coinManager, App.walletManager, App.adapterManager)
            val presenter = AddressFormatSettingsPresenter(view, router, interactor, coinTypes, showDoneButton)

            return presenter as T
        }
    }
}
