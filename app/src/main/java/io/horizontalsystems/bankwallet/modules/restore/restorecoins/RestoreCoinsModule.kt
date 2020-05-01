package io.horizontalsystems.bankwallet.modules.restore.restorecoins

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.bankwallet.modules.createwallet.view.CoinManageViewItem

object RestoreCoinsModule {

    interface IView {
        fun setItems(coinViewItems: List<CoinManageViewItem>)
        fun setProceedButton(enabled: Boolean)
        fun showDerivationSelectorDialog(derivationOptions: List<Derivation>, selected: Derivation, coin: Coin)
    }

    interface IRouter {
        fun close()
        fun closeWithSelectedCoins(enabledCoins: MutableList<Coin>)
    }

    interface IViewDelegate {
        fun onLoad()
        fun onEnable(coin: Coin)
        fun onDisable(coin: Coin)
        fun onProceedButtonClick()
        fun onCancelDerivationSelectorDialog(coin: Coin)
        fun onSelectDerivationSetting(coin: Coin, derivation: Derivation)
    }

    interface IInteractor {
        val coins: List<Coin>
        val featuredCoins: List<Coin>

        fun derivationSettings(coin: Coin): DerivationSetting?
        fun saveDerivationSetting(setting: DerivationSetting)
    }

    class Factory(private val predefinedAccountType: PredefinedAccountType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RestoreCoinsView()
            val router = RestoreCoinsRouter()
            val interactor = RestoreCoinsInteractor(App.appConfigProvider, App.blockchainSettingsManager)
            val presenter = RestoreCoinsPresenter(view, router, interactor, predefinedAccountType)

            return presenter as T
        }
    }

    fun startForResult(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType) {
        val intent = Intent(context, RestoreCoinsActivity::class.java)
        intent.putParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType)
        context.startActivityForResult(intent, ModuleCode.RESTORE_COINS)
    }

}
