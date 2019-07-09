package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.AccountType
import io.horizontalsystems.bankwallet.core.DefaultAccountType
import io.horizontalsystems.bankwallet.core.IPredefinedAccountType

class Words12AccountType : IPredefinedAccountType {
    override val title = "12 Words Key"
    override val coinCodes = "BTC, BCH, DASH, ETH, ERC-20"
    override val defaultAccountType: DefaultAccountType?
        get() = DefaultAccountType.Mnemonic(12)

    override fun supports(accountType: AccountType): Boolean {
        if (accountType is AccountType.Mnemonic) {
            return accountType.words.size == 12
        }

        return false
    }
}

class Words24AccountType : IPredefinedAccountType {
    override val title = "24 Words Key"
    override val coinCodes = "BNB, CHN"
    override val defaultAccountType: DefaultAccountType?
        get() = DefaultAccountType.Mnemonic(24)

    override fun supports(accountType: AccountType): Boolean {
        if (accountType is AccountType.Mnemonic) {
            return accountType.words.size == 24
        }

        return false
    }
}


class EosAccountType : IPredefinedAccountType {
    override val title = "Eos Account"
    override val coinCodes = "EOS"
    override val defaultAccountType: DefaultAccountType?
        get() = null

    override fun supports(accountType: AccountType): Boolean {
        return accountType is AccountType.Eos
    }
}
