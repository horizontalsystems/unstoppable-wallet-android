package io.horizontalsystems.bankwallet.modules.backup.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.SingleLiveEvent

class BackupWordsViewModel : ViewModel(), BackupWordsModule.IView, BackupWordsModule.IRouter {

    lateinit var delegate: BackupWordsModule.IViewDelegate

    val loadPageLiveEvent = SingleLiveEvent<Int>()
    val errorLiveData = SingleLiveEvent<Int>()
    val wordsLiveData = MutableLiveData<Array<String>>()
    val wordIndexesToConfirmLiveData = MutableLiveData<List<Int>>()
    val validateWordsLiveEvent = SingleLiveEvent<Unit>()
    val notifyBackedUpEvent = SingleLiveEvent<Unit>()
    val notifyClosedEvent = SingleLiveEvent<Unit>()
    val closeLiveEvent = SingleLiveEvent<Unit>()

    fun init(words: Array<String>, backedUp: Boolean) {
        BackupWordsModule.init(this, this, words, backedUp)
    }

    // view

    override fun loadPage(page: Int) {
        loadPageLiveEvent.value = page
    }

    override fun showWords(words: Array<String>) {
        wordsLiveData.value = words
    }

    override fun showConfirmationWords(indexes: List<Int>) {
        wordIndexesToConfirmLiveData.value = indexes
    }

    override fun showConfirmationError() {
        errorLiveData.value = R.string.Backup_Confirmation_FailureAlertText
    }

    override fun validateWords() {
        validateWordsLiveEvent.call()
    }

    // router

    override fun notifyBackedUp() {
        notifyBackedUpEvent.call()
    }

    override fun notifyClosed() {
        notifyClosedEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
