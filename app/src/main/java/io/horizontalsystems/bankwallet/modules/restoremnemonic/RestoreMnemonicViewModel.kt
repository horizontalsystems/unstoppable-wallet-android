package io.horizontalsystems.bankwallet.modules.restoremnemonic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class RestoreMnemonicViewModel(private val service: RestoreMnemonicService, private val clearables: List<Clearable>) : ViewModel() {

    private val disposables = CompositeDisposable()

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

    fun onTextChange(text: String, cursorPosition: Int) {
        syncState(text)

        val nonCursorInvalidItems = state.invalidItems.filter {
            val hasCursor = it.range.contains(cursorPosition - 1)

            !hasCursor || !service.isWordPartiallyValid(it.word)
        }

        invalidRangesLiveData.postValue(nonCursorInvalidItems.map { it.range })
    }

    fun onProceed() {
        if (state.invalidItems.isNotEmpty()) {
            invalidRangesLiveData.postValue(state.invalidItems.map { it.range })
            return
        }

        try {
            val accountType = service.accountType(state.allItems.map { it.word })

            proceedLiveEvent.postValue(accountType)
        } catch (error: Throwable) {
            errorLiveData.postValue(error)
        }
    }

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)

}
