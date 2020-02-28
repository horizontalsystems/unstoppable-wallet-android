package io.horizontalsystems.bankwallet.modules.base

import android.util.Log
import androidx.lifecycle.ViewModel

class BasePresenter(
        val view: BaseView,
        val router: BaseRouter,
        private val interactor: BaseInteractor) : ViewModel(), BaseModule.ViewDelegate, BaseModule.InteractorDelegate {

    override fun viewDidLoad() {
        Log.e("BasePresenter", "viewDidLoad")
        interactor.subscribeToEvents()
    }

    override fun showTorConnectionStatus() {
        view.showTorConnectionStatus()
    }

    override fun onCleared() {
        super.onCleared()
        interactor.clear()
    }
}
