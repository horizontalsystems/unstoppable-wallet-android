package io.horizontalsystems.bankwallet.modules.reportproblem

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object ReportProblemModule {
    interface IView {
        fun setEmail(email: String)
        fun setTelegramGroup(group: String)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun didTapEmail()
        fun didTapTelegram()
        fun didTapAppStatus()
    }

    interface IInteractor {
        val email: String
        val telegramGroup: String
        fun copyToClipboard(value: String)
    }

    interface IRouter {
        fun openSendMail(recipient: String)
        fun openTelegram(group: String)
        fun openAppStatus()
    }

    interface IRouterDelegate {
        fun didFailSendMail()
    }

    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = ReportProblemView()
            val router = ReportProblemRouter()
            val interactor = ReportProblemInteractor(App.appConfigProvider, TextHelper)
            val presenter = ReportProblemPresenter(view, router, interactor)

            return presenter as T
        }
    }

    fun start(context: Activity) {
        val intent = Intent(context, ReportProblemActivity::class.java)
        context.startActivity(intent)
    }
}
