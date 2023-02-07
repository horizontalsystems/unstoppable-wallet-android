package cash.p.terminal.modules.restoreaccount.restoremnemonicnonstandard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.AccountType
import cash.p.terminal.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule
import io.horizontalsystems.hdwalletkit.Language

object RestoreMnemonicNonStandardModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreMnemonicNonStandardViewModel(
                App.accountFactory,
                App.wordsManager,
                App.thirdKeyboardStorage,
            ) as T
        }
    }

    data class UiState(
        val passphraseEnabled: Boolean,
        val passphraseError: String?,
        val invalidWordRanges: List<IntRange>,
        val error: String?,
        val accountType: AccountType?,
        val wordSuggestions: RestoreMnemonicModule.WordSuggestions?,
        val language: Language,
    )
}
