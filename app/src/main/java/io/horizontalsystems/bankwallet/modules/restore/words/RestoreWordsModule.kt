package io.horizontalsystems.bankwallet.modules.restore.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType

object RestoreWordsModule {
    interface IRestoreWordsService{
        @Throws
        fun accountType(words: List<String>): AccountType
    }

    class Factory(private val wordsCount: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreWordsService(wordsCount, App.wordsManager)

            return RestoreWordsViewModel(service, listOf(service)) as T
        }
    }
}
