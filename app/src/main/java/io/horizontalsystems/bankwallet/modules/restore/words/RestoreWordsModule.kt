package io.horizontalsystems.bankwallet.modules.restore.words

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.horizontalsystems.bankwallet.core.App

object RestoreWordsModule {
    const val WORDS_COUNT = "WORDS_COUNT"

    interface View {
        fun showError(error: Int)
    }

    interface ViewDelegate {
        val words: List<String>
        fun onChange(position: Int, word: String)
        fun onDone()
    }

    interface Interactor {
        fun validate(words: List<String>)
    }

    interface InteractorDelegate {
        fun didFailToValidate(exception: Exception)
        fun didValidate(words: List<String>)
    }

    interface Router {
        fun startSyncModeModule(words: List<String>)
    }

    fun startForResult(context: AppCompatActivity, wordsCount: Int, requestCode: Int) {
        val intent = Intent(context, RestoreWordsActivity::class.java).apply {
            putExtra(WORDS_COUNT, wordsCount)
        }

        context.startActivityForResult(intent, requestCode)
    }

    fun init(view: RestoreWordsViewModel, router: Router, wordsCount: Int) {
        val interactor = RestoreWordsInteractor(App.wordsManager)
        val presenter = RestoreWordsPresenter(wordsCount, interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }
}
