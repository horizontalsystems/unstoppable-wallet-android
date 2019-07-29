package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.DefaultAccountType
import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import java.io.Serializable
import java.math.BigDecimal

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()

    class Erc20(val address: String, val decimal: Int, val fee: BigDecimal = BigDecimal.ZERO) : CoinType()
    class Eos(val token: String, val symbol: String) : CoinType()

    fun canSupport(accountType: AccountType): Boolean {
        when (this) {
            is Eos -> {
                return accountType is AccountType.Eos
            }
            is Bitcoin, BitcoinCash, Dash, Ethereum, is Erc20 -> {
                if (accountType is AccountType.Mnemonic) {
                    return accountType.words.size == 12 && accountType.derivation == Derivation.bip44
                }
            }
        }

        return false
    }

    val defaultAccountType: DefaultAccountType
        get() = when (this) {
            is Erc20, Bitcoin, BitcoinCash, Dash, Ethereum -> {
                DefaultAccountType.Mnemonic(wordsCount = 12)
            }
            is Eos -> DefaultAccountType.Eos()
        }
}
