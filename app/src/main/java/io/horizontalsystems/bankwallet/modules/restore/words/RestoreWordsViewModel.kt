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
    val errorLiveData = MutableLiveData<Throwable>()
    val wordCount = service.wordCount
    val hasAdditionalInfo = service.hasAdditionalInfo

    override fun onCleared() {
        clearables.forEach {
            it.clear()
        }
        super.onCleared()
    }

    fun onProceed(text: String?, additionalInfo: String? = null) {
        try {
            if (text.isNullOrEmpty()) {
                throw WordsError.EmptyWords
            }

            val words = text
                    .trim()
                    .toLowerCase(Locale.ENGLISH)
                    .replace(Regex("(\\s)+"), " ")
                    .split(" ")

            val accountType = service.accountType(words, additionalInfo)
            accountTypeLiveEvent.postValue(accountType)

        } catch (error: Throwable) {
            errorLiveData.postValue(error)
        }
    }


    sealed class WordsError : Exception() {
        object EmptyWords : WordsError()
    }

}
