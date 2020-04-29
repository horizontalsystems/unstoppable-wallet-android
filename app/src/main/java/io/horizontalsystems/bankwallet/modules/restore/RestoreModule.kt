package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.EosUnsupportedException
import io.horizontalsystems.bankwallet.core.putParcelableExtra
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.entities.*
import kotlinx.android.parcel.Parcelize

object RestoreModule {

    interface IView {
        fun reload(items: List<PredefinedAccountType>)
        fun showError(ex: Exception)
    }

    interface ViewDelegate {
        val items: List<PredefinedAccountType>

        fun onLoad()
        fun onSelect(predefinedAccountType: PredefinedAccountType)
        fun didEnterValidAccount(accountType: AccountType)
        fun didReturnFromCoinSettings()
        fun didReturnFromRestoreCoins(enabledCoins: List<Coin>?)
        fun onReturnWithCancel()
    }

    interface IInteractor {
        @Throws(EosUnsupportedException::class)
        fun createAccounts(accounts: List<Account>)
        @Throws
        fun account(accountType: AccountType) : Account
        fun saveWallets(wallets: List<Wallet>)
        fun create(account: Account)
        fun initializeSettings(coinType: CoinType)
    }

    interface IRouter {
        fun showRestoreCoins(predefinedAccountType: PredefinedAccountType)
        fun showKeyInput(predefinedAccountType: PredefinedAccountType)
        fun startMainModule()
        fun close()
        fun closeWithSuccess()
    }

    class Factory(
            private val predefinedAccountType: PredefinedAccountType?,
            private val restoreMode: RestoreMode
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RestoreView()
            val router = RestoreRouter()
            val interactor = RestoreInteractor(App.accountCreator, App.accountManager, App.walletManager, App.blockchainSettingsManager)
            val presenter = RestorePresenter(view, router, interactor, App.predefinedAccountTypeManager, predefinedAccountType, restoreMode)

            return presenter as T
        }
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, RestoreActivity::class.java))
    }

    fun startForResult(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType, restoreMode: RestoreMode) {
        val intent = Intent(context, RestoreActivity::class.java)
        intent.putParcelableExtra(ModuleField.PREDEFINED_ACCOUNT_TYPE, predefinedAccountType)
        intent.putParcelableExtra(ModuleField.RESTORE_MODE, restoreMode)
        context.startActivityForResult(intent, ModuleCode.RESTORE)
    }

}

@Parcelize
enum class RestoreMode(val value: String) : Parcelable {
    FromWelcome("FromWelcome"),
    FromManageKeys("FromManageKeys"),
    InApp("InApp")
}
