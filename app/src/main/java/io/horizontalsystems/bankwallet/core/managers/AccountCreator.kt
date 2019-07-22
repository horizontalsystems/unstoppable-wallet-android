package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(
        private val accountManager: IAccountManager,
        private val accountFactory: IAccountFactory,
        private val wordsManager: IWordsManager,
        private val defaultWalletCreator: DefaultWalletCreator)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?, createDefaultWallets: Boolean): Account {
        return createAccount(accountType, isBackedUp = true, defaultSyncMode = syncMode, createDefaultWallets = createDefaultWallets)
    }

    override fun createNewAccount(defaultAccountType: DefaultAccountType, createDefaultWallets: Boolean): Account {
        return createAccount(createAccountType(defaultAccountType), isBackedUp = false, defaultSyncMode = SyncMode.NEW, createDefaultWallets = createDefaultWallets)
    }

    private fun createAccount(accountType: AccountType, isBackedUp: Boolean, defaultSyncMode: SyncMode?, createDefaultWallets: Boolean): Account {
        val account = accountFactory.account(accountType, isBackedUp, defaultSyncMode)

        accountManager.create(account)

        if (createDefaultWallets) {
            defaultWalletCreator.handleCreate(account)
        }

        return account
    }

    private fun createAccountType(defaultAccountType: DefaultAccountType): AccountType {
        return when (defaultAccountType) {
            is DefaultAccountType.Mnemonic -> createMnemonicAccountType(defaultAccountType.wordsCount)
            is DefaultAccountType.Eos -> throw Exception("Eos accounts not supported yet")
        }
    }

    private fun createMnemonicAccountType(wordsCount: Int): AccountType {
        val words = wordsManager.generateWords(wordsCount)
        return AccountType.Mnemonic(words, AccountType.Derivation.bip44, "")
    }
}
