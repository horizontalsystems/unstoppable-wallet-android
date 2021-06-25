package io.horizontalsystems.bankwallet.modules.showkey

import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.toHexString
import io.horizontalsystems.ethereumkit.core.EthereumKit

class ShowKeyService(
        private val account: Account,
        private val pinComponent: IPinComponent,
        private val ethereumKitManager: EvmKitManager,
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
        get() = EthereumKit.privateKey(words, passphrase, accountSettingManager.ethereumNetwork(account).networkType).toByteArray().toHexString()

    val binanceSmartChainPrivateKey: String
        get() = EthereumKit.privateKey(words, passphrase, accountSettingManager.binanceSmartChainNetwork(account).networkType).toByteArray().toHexString()

}
