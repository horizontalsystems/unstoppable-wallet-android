package io.horizontalsystems.bankwallet.modules.restore.eos

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

object RestoreEosModule {

    interface IView

    interface IViewDelegate

    interface IInteractor

    interface IInteractorDelegate

    interface IRouter

    fun startForResult(context: AppCompatActivity, requestCode: Int) {
        val intent = Intent(context, RestoreEosActivity::class.java)
        context.startActivityForResult(intent, requestCode)
    }

    fun init(view: RestoreEosViewModel, router: IRouter) {
        val interactor = RestoreEosInteractor()
        val presenter = RestoreEosPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
