package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class BackupConfirmKeyViewModel(
        private val service: BackupConfirmKeyService,
        private val translator: Translator
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val indexViewItemLiveData = MutableLiveData<IndexViewItem>()
    val errorLiveEvent = SingleLiveEvent<String>()
    val successLiveEvent = SingleLiveEvent<Unit>()

    init {
        service.indexItemObservable
                .subscribeIO { sync(it) }
                .let { disposable.add(it) }
        sync(service.indexItem)
    }

    private fun sync(indexItem: BackupConfirmKeyService.IndexItem) {
        val indexViewItem = IndexViewItem(first = "${indexItem.first + 1}.", second = "${indexItem.second + 1}.")
        indexViewItemLiveData.postValue(indexViewItem)
    }

    private fun getErrorText(error: Throwable): String {
        return if (error is BackupConfirmKeyService.BackupValidationException) {
            translator.getString(R.string.BackupConfirmKey_EmptyOrInvalidWords)
        } else {
            error.javaClass.simpleName
        }
    }

    fun onViewCreated() {
        service.generateIndices()
    }

    fun onClickDone(firstWord: String, secondWord: String) {
        try {
            service.backup(firstWord, secondWord)
            successLiveEvent.postValue(Unit)
        } catch (error: Throwable) {
            errorLiveEvent.postValue(getErrorText(error))
        }
    }

    override fun onCleared() {
        disposable.clear()
    }

    data class IndexViewItem(val first: String, val second: String)

}
