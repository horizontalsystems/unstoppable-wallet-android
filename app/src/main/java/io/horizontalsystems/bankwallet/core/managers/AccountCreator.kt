package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(
        private val accountManager: IAccountManager,
        private val accountFactory: IAccountFactory,
        private val wordsManager: IWordsManager,
        private val defaultWalletCreator: DefaultWalletCreator)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?, createDefaultWallets: Boolean): Account {
        val account = createAccount(accountType, isBackedUp = true, defaultSyncMode = syncMode)
        if (createDefaultWallets) {
            defaultWalletCreator.handleCreate(account)
        }
        return account
    }

    override fun createNewAccount(defaultAccountType: DefaultAccountType, createDefaultWallets: Boolean): Account {
        val account = createAccount(defaultAccountType)
        if (createDefaultWallets) {
            defaultWalletCreator.handleCreate(account)
        }
        return account
    }

    override fun createNewAccount(coin: Coin) {
        val account = createAccount(coin.type.defaultAccountType)
        defaultWalletCreator.createWallet(account, coin)
    }

    private fun createAccount(defaultAccountType: DefaultAccountType): Account {
        return createAccount(createAccountType(defaultAccountType), isBackedUp = false, defaultSyncMode = SyncMode.NEW)
    }

    private fun createAccount(accountType: AccountType, isBackedUp: Boolean, defaultSyncMode: SyncMode?): Account {
        val account = accountFactory.account(accountType, isBackedUp, defaultSyncMode)

        accountManager.create(account)

        return account
    }

    private fun createAccountType(defaultAccountType: DefaultAccountType): AccountType {
        return when (defaultAccountType) {
            is DefaultAccountType.Mnemonic -> createMnemonicAccountType(defaultAccountType.wordsCount)
            is DefaultAccountType.Eos -> throw EosUnsupportedException()
        }
    }

    private fun createMnemonicAccountType(wordsCount: Int): AccountType {
        val words = wordsManager.generateWords(wordsCount)
        val derivation = if (wordsCount == 12) AccountType.Derivation.bip49 else AccountType.Derivation.bip44
        return AccountType.Mnemonic(words, derivation, salt = null)
    }
}
