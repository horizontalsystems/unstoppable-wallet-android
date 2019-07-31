package io.horizontalsystems.bankwallet.modules.restore.words

import io.horizontalsystems.bankwallet.R

class RestoreWordsPresenter(
        wordsCount: Int,
        private val interactor: RestoreWordsModule.Interactor,
        private val router: RestoreWordsModule.Router)
    : RestoreWordsModule.ViewDelegate, RestoreWordsModule.InteractorDelegate {

    var view: RestoreWordsModule.View? = null

    //  View Delegate

    override val words = MutableList(wordsCount) { "" }

    override fun onChange(position: Int, word: String) {
        words[position] = word
    }

    override fun onDone() {
        interactor.validate(words)
    }

    //  Interactor Delegate

    override fun didFailToValidate(exception: Exception) {
        view?.showError(R.string.Restore_ValidationFailed)
    }

    override fun didValidate(words: List<String>) {
        router.startSyncModeModule(words)
    }
}
