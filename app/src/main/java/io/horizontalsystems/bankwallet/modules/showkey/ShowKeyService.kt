package io.horizontalsystems.bankwallet.modules.showkey

import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString

class ShowKeyService(
    private val account: Account,
    private val pinComponent: IPinComponent,
    private val accountSettingManager: AccountSettingManager
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
            accountSettingManager.ethereumNetwork(account).chain
        ).toByteArray().toHexString()

    val binanceSmartChainPrivateKey: String
        get() = Signer.privateKey(
            words,
            passphrase,
            accountSettingManager.binanceSmartChainNetwork(account).chain
        ).toByteArray().toHexString()

}
