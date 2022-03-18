package io.horizontalsystems.bankwallet.modules.showkey

import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString

class ShowKeyService(
    private val account: Account,
    private val pinComponent: IPinComponent,
    private val evmBlockchainManager: EvmBlockchainManager
) {
    val words: List<String>
    val passphrase: String

    init {
        if (account.type is AccountType.Mnemonic) {
            words = account.type.words
            passphrase = account.type.passphrase ?: ""
        } else {
            words = listOf()
            passphrase = ""
        }
    }

    val isPinSet: Boolean
        get() = pinComponent.isPinSet

    val ethereumPrivateKey: String
        get() = Signer.privateKey(
            words,
            passphrase,
            evmBlockchainManager.getChain(EvmBlockchain.Ethereum)
        ).toByteArray().toHexString()

}
