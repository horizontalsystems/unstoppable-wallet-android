package io.horizontalsystems.bankwallet.modules.manageaccount.backupconfirmkey

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
    private val randomProvider: IRandomProvider
) : ViewModel() {

    private val wordsIndexed: List<Pair<Int, String>>
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
            wordsIndexed = account.type.words.mapIndexed { index, s ->
                Pair(index, s)
            }

            reset()
            emitState()
        } else {
            wordsIndexed = listOf()
        }
    }

    private fun reset() {
        val wordsCountToGuess = when (wordsIndexed.size) {
            12 -> 2
            15, 18, 21 -> 3
            24 -> 4
            else -> 2
        }

        val shuffled = wordsIndexed.shuffled().take(12)
        val randomNumbers = randomProvider.getRandomNumbers(wordsCountToGuess, shuffled.size)

        hiddenWordItems = randomNumbers.map { number ->
            val wordIndexed = shuffled[number]
            HiddenWordItem(
                index = wordIndexed.first,
                word = wordIndexed.second,
                isRevealed = false
            )
        }
        wordOptions = shuffled.map {
            WordOption(it.second, true)
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
