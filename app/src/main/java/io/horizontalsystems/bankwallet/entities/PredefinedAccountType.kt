package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.DefaultAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType

class Words12AccountType : IPredefinedAccountType {
    override val title = R.string.AccountType_12Words
    override val coinCodes = R.string.AccountType_12Words_Text
    override val defaultAccountType: DefaultAccountType
        get() = DefaultAccountType.Mnemonic(12)

    override fun supports(accountType: AccountType): Boolean {
        if (accountType is AccountType.Mnemonic) {
            return accountType.words.size == 12
        }

        return false
    }
}

class Words24AccountType : IPredefinedAccountType {
    override val title = R.string.AccountType_Binance
    override val coinCodes = R.string.AccountType_Binance_Text
    override val defaultAccountType: DefaultAccountType
        get() = DefaultAccountType.Mnemonic(24)

    override fun supports(accountType: AccountType): Boolean {
        if (accountType is AccountType.Mnemonic) {
            return accountType.words.size == 24
        }

        return false
    }
}


class EosAccountType : IPredefinedAccountType {
    override val title = R.string.AccountType_Eos
    override val coinCodes = R.string.AccountType_Eos_Text
    override val defaultAccountType: DefaultAccountType
        get() = DefaultAccountType.Eos()

    override fun supports(accountType: AccountType): Boolean {
        return accountType is AccountType.Eos
    }
}
