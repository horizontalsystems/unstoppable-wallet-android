package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Wallet(
    val configuredCoin: ConfiguredCoin,
    val account: Account,
    val balance: BigDecimal = BigDecimal.ZERO,
    val balanceLocked: BigDecimal = BigDecimal.ZERO
) : Parcelable, FilterAdapter.FilterItem(configuredCoin.coin.code) {
    val coin
        get() = configuredCoin.coin

    constructor(coin: Coin, account: Account) : this(ConfiguredCoin(coin), account, BigDecimal.ZERO, BigDecimal.ZERO)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return configuredCoin == other.configuredCoin && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(configuredCoin, account)
    }
}
