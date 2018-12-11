package io.horizontalsystems.bankwallet.modules.backup

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.core.IKeyStoreSafeExecute

class BackupViewModel : ViewModel(), BackupModule.IView, BackupModule.IRouter, IKeyStoreSafeExecute {
    lateinit var delegate: BackupModule.IViewDelegate

    val errorLiveData = MutableLiveData<Int>()
    val wordsLiveData = MutableLiveData<List<String>>()
    val wordIndexesToConfirmLiveData = MutableLiveData<List<Int>>()

    val navigationWordsLiveEvent = SingleLiveEvent<Void>()
    val navigationConfirmLiveEvent = SingleLiveEvent<Void>()
    val closeLiveEvent = SingleLiveEvent<Void>()
    val navigateBackLiveEvent = SingleLiveEvent<Void>()
    val navigateToSetPinLiveEvent = SingleLiveEvent<Void>()
    val showConfirmationCheckDialogLiveEvent = SingleLiveEvent<Void>()
    val keyStoreSafeExecute = SingleLiveEvent<Triple<Runnable, Runnable?, Runnable?>>()

    fun init(dismissMode: BackupPresenter.DismissMode) {
        BackupModule.init(this, this, this, dismissMode)
    }

    // view

    fun showWordsView() {
        navigationWordsLiveEvent.call()
    }

    override fun showWords(words: List<String>) {
        wordsLiveData.value = words
    }

    override fun showConfirmationWithIndexes(indexes: List<Int>) {
        wordIndexesToConfirmLiveData.value = indexes
        navigationConfirmLiveEvent.call()
    }

    override fun hideWords() {
        navigateBackLiveEvent.call()
    }

    override fun hideConfirmation() {
        errorLiveData.value = null
        navigateBackLiveEvent.call()
    }

    override fun showConfirmationError() {
        errorLiveData.value = R.string.Backup_Confirmation_FailureAlertText
    }

    override fun showTermsConfirmDialog() {
        showConfirmationCheckDialogLiveEvent.call()
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
