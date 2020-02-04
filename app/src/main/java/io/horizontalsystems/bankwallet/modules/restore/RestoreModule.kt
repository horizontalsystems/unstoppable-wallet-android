package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosModule
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule

object RestoreModule {

    interface IView {
        fun reload(items: List<PredefinedAccountType>)
        fun showError(ex: Exception)
    }

    interface ViewDelegate {
        val items: List<PredefinedAccountType>

        fun onLoad()
        fun onSelect(predefinedAccountType: PredefinedAccountType)
        fun onClickClose()
        fun didEnterValidAccount(accountType: AccountType)
        fun didReturnFromCoinSettings()
    }

    interface IRouter {
        fun close()
        fun showRestoreCoins(predefinedAccountType: PredefinedAccountType, accountType: AccountType)
        fun showKeyInput(predefinedAccountType: PredefinedAccountType)
        fun showCoinSettings()
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, RestoreActivity::class.java))
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = RestoreView()
            val router = RestoreRouter()
            val presenter = RestorePresenter(view, router, App.predefinedAccountTypeManager)

            return presenter as T
        }
    }

    fun startForResult(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType, requestCode: Int) {
        when(predefinedAccountType){
            PredefinedAccountType.Standard -> RestoreWordsModule.startForResult(context, 12, predefinedAccountType.title, requestCode)
            PredefinedAccountType.Binance -> RestoreWordsModule.startForResult(context, 24, predefinedAccountType.title, requestCode)
            PredefinedAccountType.Eos -> RestoreEosModule.startForResult(context, requestCode)
        }
    }

}
