package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class BackupConfirmKeyViewModel(
        private val service: BackupConfirmKeyService,
        private val translator: Translator
) : ViewModel() {
    private val disposable = CompositeDisposable()

    val indexViewItemLiveData = MutableLiveData<IndexViewItem>()
    val successLiveEvent = SingleLiveEvent<Unit>()

    val firstWordCautionLiveData = MutableLiveData<Caution?>(null)
    val secondWordCautionLiveData = MutableLiveData<Caution?>(null)
    val passphraseCautionLiveData = MutableLiveData<Caution?>(null)
    val clearInputsLiveEvent = SingleLiveEvent<Unit>()

    val passpraseVisible get() = service.hasPassphrase()

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

    private fun clearInputs() {
        clearInputsLiveEvent.postValue(Unit)

        firstWordCautionLiveData.postValue(null)
        secondWordCautionLiveData.postValue(null)
        passphraseCautionLiveData.postValue(null)

        service.firstWord = ""
        service.secondWord = ""
        service.passphraseConfirm = ""
    }

    private fun getErrorText(error: Throwable): String {
        return if (error is BackupConfirmKeyService.BackupError) {
            translator.getString(R.string.BackupConfirmKey_EmptyOrInvalidWords)
        } else {
            error.javaClass.simpleName
        }
    }

    fun onViewCreated() {
        service.generateIndices()
        clearInputs()
    }

    fun onChangeFirstWord(v: String) {
        service.firstWord = v
        firstWordCautionLiveData.postValue(null)
    }

    fun onChangeSecondWord(v: String) {
        service.secondWord = v
        secondWordCautionLiveData.postValue(null)
    }

    fun onChangePassphrase(v: String) {
        service.passphraseConfirm = v
        passphraseCautionLiveData.postValue(null)
    }

    fun onClickDone() {
        try {
            service.backup()
            successLiveEvent.postValue(Unit)
        } catch (e: BackupConfirmKeyService.BackupError) {
            e.validationErrors.forEach {
                when (it) {
                    BackupConfirmKeyService.ValidationError.EmptyFirstWord -> {
                        firstWordCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_EmptyWord), Caution.Type.Error))
                    }
                    BackupConfirmKeyService.ValidationError.InvalidFirstWord -> {
                        firstWordCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_InvalidWord), Caution.Type.Error))
                    }
                    BackupConfirmKeyService.ValidationError.EmptySecondWord -> {
                        secondWordCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_EmptyWord), Caution.Type.Error))
                    }
                    BackupConfirmKeyService.ValidationError.InvalidSecondWord -> {
                        secondWordCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_InvalidWord), Caution.Type.Error))
                    }
                    BackupConfirmKeyService.ValidationError.EmptyPassphrase -> {
                        passphraseCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_EmptyPassphrase), Caution.Type.Error))
                    }
                    BackupConfirmKeyService.ValidationError.InvalidPassphrase -> {
                        passphraseCautionLiveData.postValue(Caution(Translator.getString(R.string.BackupConfirmKey_Error_InvalidPassphrase), Caution.Type.Error))
                    }
                }
            }
        } catch (e: Throwable) {

        }
    }

    override fun onCleared() {
        disposable.clear()
    }

    data class IndexViewItem(val first: String, val second: String)

}
