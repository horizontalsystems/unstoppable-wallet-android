package io.horizontalsystems.bankwallet.modules.restore.words

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import kotlinx.android.parcel.Parcelize
import kotlin.jvm.Throws

object RestoreWordsModule {
    interface IRestoreWordsService {
        val wordCount: Int
        val birthdayHeightEnabled: Boolean

        @Throws
        fun accountType(words: List<String>, additionalInfo: String?): AccountType
        fun isWordValid(word: String): Boolean
        fun isWordPartiallyValid(word: String): Boolean
    }

    class Factory(private val restoreAccountType: RestoreAccountType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreWordsService(restoreAccountType, App.wordsManager, App.zcashBirthdayProvider)

            return RestoreWordsViewModel(service, listOf(service)) as T
        }
    }

    @Parcelize
    enum class RestoreAccountType(val wordsCount: Int) : Parcelable {
        STANDARD(12),
        BINANCE(24),
        ZCASH(24)
    }

}
