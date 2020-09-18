package io.horizontalsystems.bankwallet.modules.restore.words

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent
import java.lang.Exception
import java.util.*

class RestoreWordsViewModel(
        private val service: RestoreWordsService,
        private val clearables: List<Clearable>
) : ViewModel() {

    val accountTypeLiveEvent = SingleLiveEvent<AccountType>()
    val errorLiveData = MutableLiveData<Exception>()
    val wordCount = service.wordCount

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun onProceed(text: String?) {
        try {
            if (text.isNullOrEmpty()) {
                throw WordsError.EmptyWords
            }

            val words = text
                    .trim()
                    .toLowerCase(Locale.ENGLISH)
                    .replace(Regex("(\\s)+"), " ")
                    .split(" ")

            val accountType = service.accountType(words)
            accountTypeLiveEvent.postValue(accountType)

        } catch (e: Exception) {
            errorLiveData.postValue(e)
        }
    }


    sealed class WordsError : Exception() {
        object EmptyWords : WordsError()
    }

}
