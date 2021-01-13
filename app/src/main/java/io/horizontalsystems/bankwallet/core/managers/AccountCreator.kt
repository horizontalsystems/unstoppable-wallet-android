package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountCreator
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IWordsManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class AccountCreator(
        private val accountFactory: IAccountFactory,
        private val wordsManager: IWordsManager,
        private val zcashBirthdayProvider: ZcashBirthdayProvider
) : IAccountCreator {

    override fun newAccount(predefinedAccountType: PredefinedAccountType): Account {
        val accountType = accountType(predefinedAccountType)
        return accountFactory.account(accountType, AccountOrigin.Created, false)
    }

    override fun restoredAccount(accountType: AccountType): Account {
        return accountFactory.account(accountType, AccountOrigin.Restored, true)
    }

    private fun accountType(predefinedAccountType: PredefinedAccountType): AccountType {
        return when (predefinedAccountType) {
            is PredefinedAccountType.Standard -> AccountType.Mnemonic(wordsManager.generateWords(12))
            is PredefinedAccountType.Binance -> AccountType.Mnemonic(wordsManager.generateWords(24))
            is PredefinedAccountType.Zcash -> AccountType.Zcash(wordsManager.generateWords(24), zcashBirthdayProvider.getNearestBirthdayHeight())
        }
    }

}
