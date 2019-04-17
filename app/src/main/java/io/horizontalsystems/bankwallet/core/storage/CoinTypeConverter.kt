package io.horizontalsystems.bankwallet.core.storage

import androidx.room.TypeConverter
import io.horizontalsystems.bankwallet.core.Error
import io.horizontalsystems.bankwallet.entities.CoinType

class CoinTypeConverter {

    private val bitcoinKey = "bitcoin_key"
    private val bitcoinCashKey = "bitcoin_cash_key"
    private val ethereumKey = "ethereum_key"
    private val erc20Key = "erc_20_key"


    @TypeConverter
    fun stringToCoinType(value: String): CoinType {
        return when (value) {
            bitcoinKey -> CoinType.Bitcoin
            bitcoinCashKey -> CoinType.BitcoinCash
            ethereumKey -> CoinType.Ethereum
            else -> {
                if (value.contains(erc20Key)) {
                    try {
                        val parts = value.split(";")
                        if (parts.size == 3) {
                            return CoinType.Erc20(parts[1], parts[2].toInt())
                        }
                    } catch (e: Exception) { }
                }
                throw Error.CoinTypeException()
            }
        }
    }

    @TypeConverter
    fun coinTypeToString(value: CoinType): String {
        return when (value) {
            is CoinType.Bitcoin -> bitcoinKey
            is CoinType.BitcoinCash -> bitcoinCashKey
            is CoinType.Ethereum -> ethereumKey
            is CoinType.Erc20 -> "$erc20Key;${value.address};${value.decimal}"
        }
    }
}
