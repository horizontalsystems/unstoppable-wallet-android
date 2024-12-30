package cash.p.terminal.modules.manageaccount.recoveryphrase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.wallet.Account

object RecoveryPhraseModule {
    class Factory(private val account: cash.p.terminal.wallet.Account) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecoveryPhraseViewModel(account) as T
        }
    }

    data class WordNumbered(val word: String, val number: Int)

}
