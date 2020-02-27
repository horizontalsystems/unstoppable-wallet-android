package io.horizontalsystems.bankwallet.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App

object MainModule {

    interface IView {
        fun showTorConnectionStatus()
    }

    interface IViewDelegate {
        fun viewDidLoad()
    }

    interface IInteractor {
        fun onStart()
    }

    interface IInteractorDelegate {
        fun showTorConnectionStatus()
    }

    interface IRouter

    fun init(view: MainViewModel, router: IRouter) {
        val interactor = MainInteractor(App.accountManager, App.walletManager, App.adapterManager, App.netKitManager)
        val presenter = MainPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }

    fun startAsNewTask(context: Activity, activeTab: Int? = null) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activeTab?.let {
            intent.putExtra(MainActivity.ACTIVE_TAB_KEY, it)
        }
        context.startActivity(intent)
        context.overridePendingTransition(0, 0)
    }
}
