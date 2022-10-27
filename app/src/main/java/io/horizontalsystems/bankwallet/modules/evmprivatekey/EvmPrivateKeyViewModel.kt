package io.horizontalsystems.bankwallet.modules.evmprivatekey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.toRawHexString
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmPrivateKeyViewModel(
    account: Account,
    private val evmBlockchainManager: EvmBlockchainManager
) : ViewModel() {
    private val words: List<String>

    var passphrase by mutableStateOf("")
        private set

    val ethereumPrivateKey: String
        get() = Signer.privateKey(
            words,
            passphrase,
            evmBlockchainManager.getChain(BlockchainType.Ethereum)
        ).toByteArray().let {
            if (it.size > 32) {
                it.copyOfRange(1, it.size)
            } else {
                it
            }.toRawHexString()
        }

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            passphrase = account.type.passphrase
        } else {
            words = listOf()
        }
    }

}
