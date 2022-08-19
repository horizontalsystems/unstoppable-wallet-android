package io.horizontalsystems.bankwallet.modules.backupconfirmkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IRandomProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType

class BackupConfirmKeyViewModel(
    private val account: Account,
    private val accountManager: IAccountManager,
    private val indexesProvider: IRandomProvider
) : ViewModel() {

    private val wordsOriginal: List<String>
    private var hiddenWordItems = listOf<HiddenWordItem>()
    private var wordOptions = listOf<WordOption>()
    private var currentHiddenWordItemIndex = -1
    private var confirmed = false
    private var error: Throwable? = null

    var uiState by mutableStateOf(
        BackupConfirmUiState(
            hiddenWordItems = hiddenWordItems,
            wordOptions = wordOptions,
            currentHiddenWordItemIndex = currentHiddenWordItemIndex,
            confirmed = confirmed,
            error = error,
        )
    )

    init {
        if (account.type is AccountType.Mnemonic) {
            wordsOriginal = account.type.words

            reset()
            emitState()
        } else {
            wordsOriginal = listOf()
        }
    }

    private fun reset() {
        val randomIndexes = indexesProvider.getRandomIndexes(2, wordsOriginal.size)

        hiddenWordItems = randomIndexes.map { index ->
            HiddenWordItem(
                index = index,
                word = wordsOriginal[index],
                isRevealed = false
            )
        }
        wordOptions = wordsOriginal.shuffled().map {
            WordOption(it, true)
        }
        currentHiddenWordItemIndex = 0
    }

    fun onSelectWord(wordOption: WordOption) {
        val hiddenWordItem = hiddenWordItems[currentHiddenWordItemIndex]
        if (hiddenWordItem.word != wordOption.word) {
            reset()
            error = Exception(Translator.getString(R.string.BackupConfirmKey_Error_InvalidWord))
        } else {
            hiddenWordItems = hiddenWordItems.toMutableList().apply {
                set(currentHiddenWordItemIndex, hiddenWordItem.copy(isRevealed = true))
            }

            val indexOfWordOption = wordOptions.indexOf(wordOption)
            wordOptions = wordOptions.toMutableList().apply {
                set(indexOfWordOption, wordOption.copy(enabled = false))
            }

            if (currentHiddenWordItemIndex != hiddenWordItems.lastIndex) {
                currentHiddenWordItemIndex++
            } else {
                accountManager.update(account.copy(isBackedUp = true))
                confirmed = true
            }
        }

        emitState()
    }

    fun onErrorShown() {
        error = null
        emitState()
    }

    private fun emitState() {
        uiState = BackupConfirmUiState(
            hiddenWordItems = hiddenWordItems,
            wordOptions = wordOptions,
            currentHiddenWordItemIndex = currentHiddenWordItemIndex,
            confirmed = confirmed,
            error = error
        )
    }
}

data class BackupConfirmUiState(
    val hiddenWordItems: List<HiddenWordItem>,
    val wordOptions: List<WordOption>,
    val currentHiddenWordItemIndex: Int,
    val confirmed: Boolean,
    val error: Throwable?
)

data class WordOption(val word: String, val enabled: Boolean)

data class HiddenWordItem(
    val index: Int,
    val word: String,
    val isRevealed: Boolean
) {
    override fun toString() = when {
        isRevealed -> "${index + 1}. $word"
        else -> "${index + 1}."
    }
}
