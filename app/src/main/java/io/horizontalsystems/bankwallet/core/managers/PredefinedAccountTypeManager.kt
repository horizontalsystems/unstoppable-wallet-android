package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType

class PredefinedAccountTypeManager(
        val appConfigProvider: IAppConfigProvider,
        val accountManager: IAccountManager,
        val accountCreator: IAccountCreator,
        val wordsManager: IWordsManager)
    : IPredefinedAccountTypeManager {

    override val allTypes: List<IPredefinedAccountType>
        get() = appConfigProvider.predefinedAccountTypes

    override fun account(predefinedAccountType: IPredefinedAccountType): Account? {
        return accountManager.accounts.find { predefinedAccountType.supports(it.type) }
    }

    override fun createAccount(predefinedAccountType: IPredefinedAccountType): Account? {
        val accountType = accountType(predefinedAccountType) ?: return null
        return accountCreator.createNewAccount(accountType)
    }

    override fun createAllAccounts() {
        allTypes.forEach { predefinedAccountType ->
            createAccount(predefinedAccountType)
        }
    }

    private fun accountType(predefinedAccountType: IPredefinedAccountType): AccountType? {
        val defaultAccountType = predefinedAccountType.defaultAccountType ?: return null

        return when (defaultAccountType) {
            is DefaultAccountType.Mnemonic -> createMnemonicAccountType(defaultAccountType.wordsCount)
        }
    }

    private fun createMnemonicAccountType(wordsCount: Int): AccountType {
        val words = wordsManager.generateWords(wordsCount)
        return AccountType.Mnemonic(words, AccountType.Derivation.bip44, "")
    }
}
