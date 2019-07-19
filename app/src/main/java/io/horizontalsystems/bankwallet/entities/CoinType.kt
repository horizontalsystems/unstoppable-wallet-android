package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.DefaultAccountType
import java.io.Serializable
import java.math.BigDecimal

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()

    class Erc20(val address: String, val decimal: Int, val fee: BigDecimal = BigDecimal.ZERO) : CoinType()
    class Eos(val token: String, symbol: String) : CoinType()

    fun canSupport(accountType: AccountType): Boolean {
        when (this) {
            is Bitcoin, BitcoinCash, Dash -> {
                return (accountType is AccountType.Mnemonic ||
                        accountType is AccountType.HDMasterKey)
            }
            is Ethereum,
            is Erc20 -> {
                return (accountType is AccountType.Mnemonic ||
                        accountType is AccountType.PrivateKey)
            }
            is Eos -> {
                return accountType is AccountType.Eos
            }
        }
    }

    val defaultAccountType: DefaultAccountType
        get() = when (this) {
            is Erc20, Bitcoin, BitcoinCash, Dash, Ethereum -> {
                DefaultAccountType.Mnemonic(wordsCount = 12)
            }
            is Eos -> DefaultAccountType.Eos()
        }
}
