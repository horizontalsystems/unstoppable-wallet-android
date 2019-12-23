package io.horizontalsystems.bankwallet.modules.restore

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.utils.ModuleCode
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.modules.restore.eos.RestoreEosModule
import io.horizontalsystems.bankwallet.modules.restore.words.RestoreWordsModule

object RestoreModule {

    interface View {
        fun reload(items: List<PredefinedAccountType>)
        fun showError(ex: Exception)
    }

    interface ViewDelegate {
        val items: List<PredefinedAccountType>

        fun viewDidLoad()
        fun onSelect(predefinedAccountType: PredefinedAccountType)
        fun onClickClose()
        fun onRestore()
    }

    interface Router {
        fun close()
        fun startRestoreCoins(predefinedAccountType: PredefinedAccountType)
    }

    fun start(context: Context) {
        context.startActivity(Intent(context, RestoreActivity::class.java))
    }

    fun startForResult(context: AppCompatActivity, predefinedAccountType: PredefinedAccountType) {
        when(predefinedAccountType){
            PredefinedAccountType.Standard -> RestoreWordsModule.startForResult(context, 12, predefinedAccountType.title, ModuleCode.RESTORE)
            PredefinedAccountType.Binance -> RestoreWordsModule.startForResult(context, 24, predefinedAccountType.title, ModuleCode.RESTORE)
            PredefinedAccountType.Eos -> RestoreEosModule.startForResult(context, predefinedAccountType.title, ModuleCode.RESTORE)
        }
    }

    fun init(view: RestoreViewModel, router: Router) {
        val presenter = RestorePresenter(router, App.predefinedAccountTypeManager)

        view.delegate = presenter
        presenter.view = view
    }
}
