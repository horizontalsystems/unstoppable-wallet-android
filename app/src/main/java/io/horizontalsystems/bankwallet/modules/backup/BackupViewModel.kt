package io.horizontalsystems.bankwallet.modules.backup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent

class BackupViewModel : ViewModel(), BackupModule.IView, BackupModule.IRouter {

    lateinit var delegate: BackupModule.IViewDelegate

    val loadPageLiveEvent = SingleLiveEvent<Int>()
    val errorLiveData = SingleLiveEvent<Int>()
    val wordsLiveData = MutableLiveData<List<String>>()
    val wordIndexesToConfirmLiveData = MutableLiveData<List<Int>>()
    val validateWordsLiveEvent = SingleLiveEvent<Void>()
    val closeLiveEvent = SingleLiveEvent<Void>()

    fun init(accountId: String) {
        BackupModule.init(this, this, accountId)
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

    override fun close() {
        closeLiveEvent.call()
    }
}
