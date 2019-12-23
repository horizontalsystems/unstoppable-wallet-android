package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.InvalidMnemonicWordsCountException

class RestoreWordsPresenter(
        private val wordsCount: Int,
        private val interactor: RestoreWordsModule.Interactor,
        private val router: RestoreWordsModule.Router)
    : RestoreWordsModule.ViewDelegate, RestoreWordsModule.InteractorDelegate {

    var view: RestoreWordsModule.View? = null

    //  IView Delegate

    override val words = mutableListOf<String>()

    override fun onDone(wordsString: String?) {
        val wordList = wordsString?.split(" ") ?: return
        words.clear()
        words.addAll(wordList)
        validate()
    }

    //  Interactor Delegate

    override fun didValidate() {
        router.notifyRestored()
    }

    override fun didFailToValidate(exception: Exception) {
        view?.showError(R.string.Restore_ValidationFailed)
    }

    private fun validate(){
        if (words.size != wordsCount) {
            didFailToValidate(InvalidMnemonicWordsCountException())
        } else {
            interactor.validate(words)
        }
    }
}
