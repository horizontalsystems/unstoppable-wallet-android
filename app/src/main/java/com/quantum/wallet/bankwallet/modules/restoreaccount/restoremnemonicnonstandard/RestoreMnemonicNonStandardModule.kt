package com.quantum.wallet.bankwallet.modules.restoreaccount.restoremnemonicnonstandard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.AccountType
import com.quantum.wallet.bankwallet.modules.restoreaccount.restoremnemonic.RestoreMnemonicModule
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
