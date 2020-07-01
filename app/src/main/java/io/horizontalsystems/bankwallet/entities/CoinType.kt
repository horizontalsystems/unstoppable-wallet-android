package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

sealed class CoinType : Parcelable {
    @Parcelize object Bitcoin : CoinType()
    @Parcelize object Litecoin : CoinType()
    @Parcelize object BitcoinCash : CoinType()
    @Parcelize object Dash : CoinType()
    @Parcelize object Ethereum : CoinType()

    @Parcelize class Erc20(val address: String, val fee: BigDecimal = BigDecimal.ZERO, val minimumRequiredBalance: BigDecimal = BigDecimal.ZERO, val minimumSendAmount: BigDecimal = BigDecimal.ZERO) : CoinType()
    @Parcelize class Eos(val token: String, val symbol: String) : CoinType()
    @Parcelize class Binance(val symbol: String) : CoinType()

    fun canSupport(accountType: AccountType): Boolean {
        when (this) {
            is Eos -> {
                return accountType is AccountType.Eos
            }
            is Bitcoin,
            is Litecoin,
            is BitcoinCash,
            is Dash,
            is Ethereum,
            is Erc20 -> {
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

    fun typeLabel(): String? {
        return when (this) {
            is Erc20 -> "ERC20"
            is Eos -> if (symbol != "EOS") "EOSIO" else null
            is Binance -> if (symbol != "BNB") "BEP2" else null
            else -> null
        }
    }

    val predefinedAccountType: PredefinedAccountType
        get() = when (this) {
            is Bitcoin,
            is Litecoin,
            is Erc20,
            is BitcoinCash,
            is Dash,
            is Ethereum -> PredefinedAccountType.Standard
            is Binance -> PredefinedAccountType.Binance
            is Eos -> PredefinedAccountType.Eos
        }

}

@Parcelize
enum class CoinSetting : Parcelable {
    Derivation,
    SyncMode
}

typealias CoinSettings = MutableMap<CoinSetting, String>
