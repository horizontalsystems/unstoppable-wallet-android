package io.horizontalsystems.bankwallet.modules.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.RateUsType
import io.horizontalsystems.bankwallet.entities.Account

object MainModule {

    interface IView {
        fun showRateApp()
        fun openPlayMarket()
        fun hideContent(hide: Boolean)
        fun toggleBagdeVisibility(visible: Boolean)
        fun setTransactionTabEnabled(enabled: Boolean)
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onClear()
        fun onResume()
    }

    interface IInteractor {
        fun onStart()
        val allBackedUp: Boolean
        val termsAccepted: Boolean
        val isPinSet: Boolean
        fun clear()
    }

    interface IInteractorDelegate {
        fun updateBadgeVisibility()
        fun showRateApp(showRateUs: RateUsType)
        fun sync(accounts: List<Account>)
    }

    fun init(view: MainViewModel) {
        val interactor = MainInteractor(App.rateAppManager, App.backupManager, App.termsManager, App.pinComponent, App.accountManager)
        val presenter = MainPresenter(App.pinComponent, interactor)

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
