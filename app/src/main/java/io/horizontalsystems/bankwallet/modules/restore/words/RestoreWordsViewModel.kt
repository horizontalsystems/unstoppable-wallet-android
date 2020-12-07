package io.horizontalsystems.bankwallet.modules.restore.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent
import java.util.*

class RestoreWordsViewModel(private val service: RestoreWordsService, private val clearables: List<Clearable>) : ViewModel() {

    val accountTypeLiveEvent = SingleLiveEvent<AccountType>()
    val errorLiveData = MutableLiveData<Throwable>()
    val wordCount = service.wordCount

    val birthdayHeightEnabled = service.birthdayHeightEnabled
    val invalidRanges = MutableLiveData<List<IntRange>>()

    private val regex = Regex("\\S+")
    private var state = State(listOf(), listOf())

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun onProceed(additionalInfo: String? = null) {
        if (state.invalidItems.isNotEmpty()) {
            invalidRanges.postValue(state.invalidItems.map { it.range })
            return
        }

        try {
            val accountType = service.accountType(state.allItems.map { it.word }, additionalInfo)

            accountTypeLiveEvent.postValue(accountType)
        } catch (error: Throwable) {
            errorLiveData.postValue(error)
        }
    }

    fun onTextChange(text: String, cursorPosition: Int) {
        syncState(text)

        val nooCursorInvalidItems = state.invalidItems.filter {
            val hasCursor = it.range.contains(cursorPosition - 1)

            !hasCursor || !service.isWordPartiallyValid(it.word)
        }

        invalidRanges.postValue(nooCursorInvalidItems.map { it.range })
    }

    private fun syncState(text: String) {
        val allItems = wordItems(text)
        val invalidItems = allItems.filter {
            !service.isWordValid(it.word)
        }

        state = State(allItems, invalidItems)
    }

    private fun wordItems(text: String): List<WordItem> {
        return regex.findAll(text.toLowerCase(Locale.ENGLISH))
                .map { WordItem(it.value, it.range) }
                .toList()
    }

    data class WordItem(val word: String, val range: IntRange)
    data class State(val allItems: List<WordItem>, val invalidItems: List<WordItem>)
}
