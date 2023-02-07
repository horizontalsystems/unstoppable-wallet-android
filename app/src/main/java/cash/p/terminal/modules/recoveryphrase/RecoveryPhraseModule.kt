package cash.p.terminal.modules.recoveryphrase

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.entities.Account

object RecoveryPhraseModule {
    const val ACCOUNT = "account"

    class Factory(private val account: Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecoveryPhraseViewModel(account) as T
        }
    }

    fun prepareParams(account: Account) = bundleOf(ACCOUNT to account)

    data class WordNumbered(val word: String, val number: Int)

}
