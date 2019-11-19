package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.R

class RestoreWordsPresenter(
        wordsCount: Int,
        private val showSyncMode: Boolean,
        private val interactor: RestoreWordsModule.Interactor,
        private val router: RestoreWordsModule.Router)
    : RestoreWordsModule.ViewDelegate, RestoreWordsModule.InteractorDelegate {

    var view: RestoreWordsModule.View? = null

    //  IView Delegate

    override val words = MutableList(wordsCount) { "" }

    override fun onDone(wordsString: String?) {
        val wordList = wordsString?.split(" ") ?: return
        words.clear()
        words.addAll(wordList)
        interactor.validate(words)
    }

    //  Interactor Delegate

    override fun didValidate() {
        if (showSyncMode) {
            router.startSyncModeModule()
        } else {
            router.notifyRestored()
        }
    }

    override fun didFailToValidate(exception: Exception) {
        view?.showError(R.string.Restore_ValidationFailed)
    }
}
