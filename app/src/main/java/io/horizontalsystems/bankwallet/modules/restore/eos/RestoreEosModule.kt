package io.horizontalsystems.bankwallet.modules.restore.eos

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.utils.ModuleField
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

object RestoreEosModule {

    interface IView {
        fun setPrivateKey(key: String)
        fun setAccount(account: String)
        fun showError(resId: Int)
    }

    interface IViewDelegate {
        fun onClickDone(accountName: String, privateKey: String)
        fun onClickScan()
        fun onPasteAccount()
        fun onPasteKey()
        fun onQRCodeScan(key: String?)
    }

    interface IInteractor {
        val textFromClipboard: String?
        fun validate(accountName: String, privateKey: String)
    }

    interface IInteractorDelegate {
        fun onValidationSuccess(accountName: String, privateKey: String)
        fun onInvalidAccount()
        fun onInvalidKey()
    }

    interface IRouter {
        fun startQRScanner()
        fun finishWithSuccess(accountName: String, privateKey: String)
    }

    fun startForResult(context: AppCompatActivity, titleRes: Int, requestCode: Int) {
        val intent = Intent(context, RestoreEosActivity::class.java).apply {
            putExtra(ModuleField.ACCOUNT_TYPE_TITLE, titleRes)
        }
        context.startActivityForResult(intent, requestCode)
    }

    fun init(view: RestoreEosViewModel, router: IRouter) {
        val interactor = RestoreEosInteractor(TextHelper)
        val presenter = RestoreEosPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
