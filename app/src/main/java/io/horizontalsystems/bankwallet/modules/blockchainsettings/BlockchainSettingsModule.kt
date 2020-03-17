package io.horizontalsystems.bankwallet.modules.blockchainsettings

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

object BlockchainSettingsModule {

    interface IView {
        fun showDerivationChangeAlert(derivation: Derivation, coinTitle: String)
        fun showSyncModeChangeAlert(syncMode: SyncMode, coinTitle: String)
        fun setDerivation(derivation: Derivation)
        fun setSyncMode(syncMode: SyncMode)
        fun setSourceLink(coinType: CoinType)
        fun setTitle(title: String)
    }

    interface IViewDelegate {
        fun onViewLoad()
        fun onSelect(syncMode: SyncMode)
        fun onSelect(derivation: Derivation)
        fun proceedWithDerivationChange(derivation: Derivation)
        fun proceedWithSyncModeChange(syncMode: SyncMode)
    }

    interface IInteractor {
        fun coinWithSetting(coinType: CoinType): Coin?
        fun blockchainSettings(coinType: CoinType): BlockchainSetting?
        fun getWalletForUpdate(coinType: CoinType): Wallet?
        fun reSyncWallet(wallet: Wallet)
        fun updateSettings(blockchainSettings: BlockchainSetting)
    }

    interface IRouter {
        fun closeWithResultOk()
        fun close()
    }

    fun startForResult(context: AppCompatActivity, coinType: CoinType) {
        val intent = Intent(context, BlockchainSettingsActivity::class.java)
        intent.putParcelableExtra(ModuleField.COIN_TYPE, coinType)
        context.startActivityForResult(intent, ModuleCode.COIN_SETTINGS)
    }

    class Factory(private val coinType: CoinType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = BlockchainSettingsView()
            val router = BlockchainSettingsRouter()
            val interactor = BlockchainSettingsInteractor(App.blockchainSettingsManager, App.walletManager)
            val presenter = BlockchainSettingsPresenter(view, router, interactor, coinType)

            return presenter as T
        }
    }
}
