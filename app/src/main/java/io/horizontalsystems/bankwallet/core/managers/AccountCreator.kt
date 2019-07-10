package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.DefaultAccountType.Mnemonic
import io.horizontalsystems.bankwallet.core.factories.AccountFactory
import io.horizontalsystems.bankwallet.entities.SyncMode

class AccountCreator(
        private val accountManager: IAccountManager,
        private val accountFactory: AccountFactory,
        private val wordsManager: IWordsManager)
    : IAccountCreator {

    override fun createRestoredAccount(accountType: AccountType, syncMode: SyncMode?): Account {
        val account = accountFactory.account(accountType, true, syncMode ?: SyncMode.FAST)
        accountManager.save(account)
        return account
    }

    override fun createNewAccount(defaultAccountType: DefaultAccountType): Account {
        val account = when (defaultAccountType) {
            is Mnemonic -> createMnemonicAccount(defaultAccountType.wordsCount)
        }

        accountManager.save(account)
        return account
    }

    private fun createMnemonicAccount(wordsCount: Int): Account {
        val words = wordsManager.generateWords(wordsCount)
        val accountType = AccountType.Mnemonic(words, AccountType.Derivation.bip44, "")

        return accountFactory.account(
                type = accountType,
                backedUp = false,
                defaultSyncMode = SyncMode.NEW)
    }
}
