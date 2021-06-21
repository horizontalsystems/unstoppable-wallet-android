package io.horizontalsystems.bankwallet.modules.createaccount

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable

class CreateAccountViewModel(private val service: CreateAccountService, private val clearables: List<Clearable>) : ViewModel() {

    val kindLiveData = service.kindObservable
            .toFlowable(BackpressureStrategy.BUFFER)
            .map {
                it.title
            }
            .let {
                LiveDataReactiveStreams.fromPublisher(it)
            }

    val inputsVisibleLiveData = LiveDataReactiveStreams.fromPublisher(service.passphraseEnabledObservable.toFlowable(BackpressureStrategy.BUFFER))
    val passphraseCautionLiveData = MutableLiveData<Caution?>()
    val passphraseConfirmationCautionLiveData = MutableLiveData<Caution?>()
    val clearInputsLiveEvent = SingleLiveEvent<Unit>()
    val showErrorLiveEvent = SingleLiveEvent<String>()
    val finishLiveEvent = SingleLiveEvent<Unit>()

    private val disposables = CompositeDisposable()

    val kindViewItems = service.allKinds.map {
        ViewItemWrapper(it.title, it)
    }

    var selectedKindViewItem: ViewItemWrapper<CreateAccountModule.Kind>
        get() = ViewItemWrapper(service.kind.title, service.kind)
        set(value) {
            service.kind = value.item
        }

    private fun clearInputs() {
        clearInputsLiveEvent.postValue(Unit)
        clearCautions()

        service.passphrase = ""
        service.passphraseConfirmation = ""
    }

    private fun clearCautions() {
        if (passphraseCautionLiveData.value != null) {
            passphraseCautionLiveData.postValue(null)
        }

        if (passphraseConfirmationCautionLiveData.value != null) {
            passphraseConfirmationCautionLiveData.postValue(null)
        }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    fun onTogglePassphrase(enabled: Boolean) {
        service.passphraseEnabled = enabled
        clearInputs()
    }

    fun onChangePassphrase(v: String) {
        service.passphrase = v
        clearCautions()
    }

    fun onChangePassphraseConfirmation(v: String) {
        service.passphraseConfirmation = v
        clearCautions()
    }

    fun onClickCreate() {
        passphraseCautionLiveData.postValue(null)
        passphraseConfirmationCautionLiveData.postValue(null)

        try {
            service.createAccount()
            finishLiveEvent.postValue(Unit)
        } catch (t: CreateAccountService.CreateError.EmptyPassphrase) {
            passphraseCautionLiveData.postValue(Caution(Translator.getString(R.string.CreateWallet_Error_EmptyPassphrase), Caution.Type.Error))
        } catch (t: CreateAccountService.CreateError.InvalidConfirmation) {
            passphraseConfirmationCautionLiveData.postValue(Caution(Translator.getString(R.string.CreateWallet_Error_InvalidConfirmation), Caution.Type.Error))
        } catch (t: Throwable) {
            showErrorLiveEvent.postValue(t.localizedMessage ?: t.javaClass.simpleName)
        }
    }

    fun validatePassphrase(text: String?): Boolean {
        return validatePassphraseAndNotify(text, passphraseCautionLiveData)
    }

    fun validatePassphraseConfirmation(text: String?): Boolean {
        return validatePassphraseAndNotify(text, passphraseConfirmationCautionLiveData)
    }

    private fun validatePassphraseAndNotify(text: String?, cautionLiveData: MutableLiveData<Caution?>): Boolean {
        val valid = service.validatePassphrase(text)
        if (!valid) {
            cautionLiveData.postValue(Caution(Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols), Caution.Type.Error))
        }
        return valid
    }

}
