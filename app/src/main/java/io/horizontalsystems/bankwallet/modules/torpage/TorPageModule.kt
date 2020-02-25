package io.horizontalsystems.bankwallet.modules.torpage

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object TorPageModule {

    interface IView {
        fun setTorSwitch(enabled: Boolean)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onTorSwitch(enabled: Boolean)
        fun onClose()
    }

    interface IInteractor {
        fun enableTor()
        fun disableTor()
        var isTorEnabled: Boolean
    }

    interface IRouter{
        fun close()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = TorPageView()
            val router = TorPageRouter()
            val interactor = TorPageInteractor(App.netKitManager, App.localStorage)
            val presenter = TorPagePresenter(view, router, interactor)

            return presenter as T
        }
    }

    fun start(context: Context){
        val intent = Intent(context, TorPageActivity::class.java)
        context.startActivity(intent)
    }

}
