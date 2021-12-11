package io.horizontalsystems.bankwallet.modules.restore.restoremnemonic

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class RestoreMnemonicViewModel(private val service: RestoreMnemonicService, private val clearables: List<Clearable>) : ViewModel() {

    private val disposables = CompositeDisposable()

    val inputsVisibleLiveData = LiveDataReactiveStreams.fromPublisher(service.passphraseEnabledObservable.toFlowable(BackpressureStrategy.BUFFER))
    val passphraseCautionLiveData = MutableLiveData<Caution?>()
    val clearInputsLiveEvent = SingleLiveEvent<Unit>()

    val invalidRangesLiveData = MutableLiveData<List<IntRange>>()
    val proceedLiveEvent = SingleLiveEvent<AccountType>()
    val errorLiveData = MutableLiveData<Throwable>()

    private val regex = Regex("\\S+")
    private var state = State(listOf(), listOf())

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    private fun wordItems(text: String): List<WordItem> {
        return regex.findAll(text.toLowerCase(Locale.ENGLISH))
                .map { WordItem(it.value, it.range) }
                .toList()
    }

    private fun syncState(text: String) {
        val allItems = wordItems(text)
        val invalidItems = allItems.filter {
            !service.isWordValid(it.word)
        }

        state = State(allItems, invalidItems)
    }

    private fun clearInputs() {
        clearInputsLiveEvent.postValue(Unit)
        clearCautions()

        service.passphrase = ""
    }

    private fun clearCautions() {
        if (passphraseCautionLiveData.value != null) {
            passphraseCautionLiveData.postValue(null)
        }
    }

    fun onTextChange(text: String, cursorPosition: Int) {
        syncState(text)

        val nonCursorInvalidItems = state.invalidItems.filter {
            val hasCursor = it.range.contains(cursorPosition - 1)

            !hasCursor || !service.isWordPartiallyValid(it.word)
        }

        invalidRangesLiveData.postValue(nonCursorInvalidItems.map { it.range })
    }

    fun onTogglePassphrase(enabled: Boolean) {
        service.passphraseEnabled = enabled
        clearInputs()
    }

    fun onChangePassphrase(v: String) {
        service.passphrase = v
        clearCautions()
    }

    fun validatePassphrase(text: String?): Boolean {
        val valid = service.validatePassphrase(text)
        if (!valid) {
            passphraseCautionLiveData.postValue(Caution(Translator.getString(R.string.CreateWallet_Error_PassphraseForbiddenSymbols), Caution.Type.Error))
        }
        return valid
    }

    fun onProceed() {
        passphraseCautionLiveData.postValue(null)

        if (state.invalidItems.isNotEmpty()) {
            invalidRangesLiveData.postValue(state.invalidItems.map { it.range })
            return
        }

        try {
            val accountType = service.accountType(state.allItems.map { it.word })

            proceedLiveEvent.postValue(accountType)
        } catch (t: RestoreMnemonicService.RestoreError.EmptyPassphrase) {
            passphraseCautionLiveData.postValue(Caution(Translator.getString(R.string.Restore_Error_EmptyPassphrase), Caution.Type.Error))
        } catch (error: Throwable) {
            errorLiveData.postValue(error)
        }
    }

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)

}
