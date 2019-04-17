package io.horizontalsystems.bankwallet.modules.backup

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

class BackupViewModel : ViewModel(), BackupModule.IView, BackupModule.IRouter, IKeyStoreSafeExecute {
    lateinit var delegate: BackupModule.IViewDelegate

    val loadPageLiveEvent = SingleLiveEvent<Int>()
    val errorLiveData = SingleLiveEvent<Int>()
    val wordsLiveData = MutableLiveData<List<String>>()
    val wordIndexesToConfirmLiveData = MutableLiveData<List<Int>>()
    val validateWordsLiveEvent = SingleLiveEvent<Void>()
    val closeLiveEvent = SingleLiveEvent<Void>()
    val navigateToSetPinLiveEvent = SingleLiveEvent<Void>()
    val showConfirmationCheckDialogLiveEvent = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init(dismissMode: BackupPresenter.DismissMode) {
        BackupModule.init(this, this, this, dismissMode)
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

    override fun showTermsConfirmDialog() {
        showConfirmationCheckDialogLiveEvent.call()
    }

    override fun validateWords() {
        validateWordsLiveEvent.call()
    }

    // router

    override fun close() {
        closeLiveEvent.call()
    }

    override fun navigateToSetPin() {
        navigateToSetPinLiveEvent.call()
    }

    override fun safeExecute(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        keyStoreSafeExecute.value = Triple(action, onSuccess, onFailure)
    }
}
