package io.horizontalsystems.bankwallet.modules.restore.words

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.App

object RestoreWordsModule {

    interface IView {
        fun showError(error: Int)
    }

    interface IViewDelegate {
        fun restoreDidClick(words: List<String>)
    }

    interface IInteractor {
        fun validate(words: List<String>)
    }

    interface IInteractorDelegate {
        fun didFailToValidate(exception: Exception)
        fun didValidate(words: List<String>)
    }

    interface IRouter {
        fun startSyncModeModule(words: List<String>)
    }

    fun startForResult(context: AppCompatActivity, wordsCount: Int, requestCode: Int) {
        val intent = Intent(context, RestoreWordsActivity::class.java).apply {
            putExtra("wordsCount", wordsCount)
        }

        context.startActivityForResult(intent, requestCode)
    }

    fun init(view: RestoreWordsViewModel, router: IRouter) {
        val interactor = RestoreWordsInteractor(App.wordsManager)
        val presenter = RestoreWordsPresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
