package io.horizontalsystems.bankwallet.modules.backup.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent

class BackupWordsViewModel : ViewModel(), BackupWordsModule.IView, BackupWordsModule.IRouter {

    lateinit var delegate: BackupWordsModule.IViewDelegate

    val loadPageLiveEvent = SingleLiveEvent<Int>()
    val errorLiveData = SingleLiveEvent<Int>()
    val wordsLiveData = MutableLiveData<List<String>>()
    val wordIndexesToConfirmLiveData = MutableLiveData<List<Int>>()
    val validateWordsLiveEvent = SingleLiveEvent<Void>()
    val notifyBackedUpEvent = SingleLiveEvent<String>()
    val startPinModuleEvent = SingleLiveEvent<Void>()
    val closeLiveEvent = SingleLiveEvent<Void>()

    fun init(accountId: String, words: List<String>) {
        BackupWordsModule.init(this, this, accountId, words)
    }

    // view

    override fun loadPage(page: Int) {
        loadPageLiveEvent.value = page
    }

    override fun showWords(words: List<String>) {
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

    override fun startPinModule() {
        startPinModuleEvent.call()
    }

    override fun notifyBackedUp(accountId: String) {
        notifyBackedUpEvent.value = accountId
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
