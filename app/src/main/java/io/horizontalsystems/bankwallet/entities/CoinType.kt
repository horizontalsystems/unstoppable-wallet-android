package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.DefaultAccountType
import java.io.Serializable
import java.math.BigDecimal

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()

    class Erc20(val address: String, val fee: BigDecimal = BigDecimal.ZERO, val gasLimit: Long = 100_000, val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO) : CoinType()
    class Eos(val token: String, val symbol: String) : CoinType()
    class Binance(val symbol: String) : CoinType()

    fun canSupport(accountType: AccountType): Boolean {
        when (this) {
            is Eos -> {
                return accountType is AccountType.Eos
            }
            is Bitcoin, BitcoinCash, Dash, Ethereum, is Erc20 -> {
                if (accountType is AccountType.Mnemonic) {
                    return accountType.words.size == 12 && accountType.salt == null
                }
            }
            is Binance -> {
                if (accountType is AccountType.Mnemonic) {
                    return accountType.words.size == 24 && accountType.salt == null
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
            is Binance -> DefaultAccountType.Mnemonic(wordsCount = 24)
            is Eos -> DefaultAccountType.Eos()
        }
}
