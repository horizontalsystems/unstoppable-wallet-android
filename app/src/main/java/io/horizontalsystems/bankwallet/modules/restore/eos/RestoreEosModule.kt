package io.horizontalsystems.bankwallet.modules.restore.eos

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

object RestoreEosModule {

    interface IView

    interface IViewDelegate {
        fun onClickDone(accountName: String, privateKey: String)
    }

    interface IInteractor {
        fun validate(accountName: String, privateKey: String)
    }

    interface IInteractorDelegate {
        fun onValidationSuccess(accountName: String, privateKey: String)
        fun onValidationFail(error: Exception)
    }

    interface IRouter {
        fun finishWithSuccess(accountName: String, privateKey: String)
    }

    fun startForResult(context: AppCompatActivity, requestCode: Int) {
        context.startActivityForResult(Intent(context, RestoreEosActivity::class.java), requestCode)
    }

    fun init(view: RestoreEosViewModel, router: IRouter) {
        val interactor = RestoreEosInteractor()
        val presenter = RestoreEosPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
