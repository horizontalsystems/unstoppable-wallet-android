package io.horizontalsystems.bankwallet.modules.showkey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString

class ShowKeyViewModel(
    account: Account,
    pinComponent: IPinComponent,
    private val evmBlockchainManager: EvmBlockchainManager
) : ViewModel() {
    private val words: List<String>

    var passphrase by mutableStateOf("")
        private set

    var viewState by mutableStateOf(ShowKeyModule.ViewState.Warning)
        private set

    var wordsNumbered by mutableStateOf<List<ShowKeyModule.WordNumbered>>(listOf())
        private set

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            wordsNumbered = words.mapIndexed { index, word ->
                ShowKeyModule.WordNumbered(word, index + 1)
            }
            passphrase = account.type.passphrase
        } else {
            words = listOf()
        }
    }

    val isPinSet = pinComponent.isPinSet

    val ethereumPrivateKey: String
        get() = Signer.privateKey(
            words,
            passphrase,
            evmBlockchainManager.getChain(EvmBlockchain.Ethereum)
        ).toByteArray().let {
            if (it.size > 32) {
                it.copyOfRange(1, it.size)
            } else {
                it
            }.toHexString()
        }

    fun showKey() {
        viewState = ShowKeyModule.ViewState.Key
    }

}
