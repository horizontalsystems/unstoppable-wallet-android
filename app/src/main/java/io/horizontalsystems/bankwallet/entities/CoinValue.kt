package io.horizontalsystems.bankwallet.entities

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.math.BigInteger

data class CoinValue(val platformCoin: PlatformCoin, val value: BigDecimal) {
    val coin = platformCoin.coin
    val decimal = platformCoin.decimals

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return platformCoin.coin.uid == other.platformCoin.coin.uid && value == other.value
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = platformCoin.coin.uid.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    fun getFormatted(maximumFractionDigits: Int = 8): String {
        return App.numberFormatter.formatCoin(value, coin.code, 0, maximumFractionDigits)
    }

    @Composable
    fun getShortenedFormatted(): String {
        val roundedForTxs = App.numberFormatter.getShortenedForTxs(value)
        val suffix = roundedForTxs.suffix.titleResId?.let {
            stringResource(it)
        } ?: ""
        return App.numberFormatter.format(
            value = roundedForTxs.value,
            minimumFractionDigits = 0,
            maximumFractionDigits = roundedForTxs.value.scale(),
            suffix = "$suffix ${coin.code}",
        )
    }

}

fun BigDecimal.isMaxValue(decimals: Int): Boolean {
    //biggest number in Ethereum platform
    val max256BitsNumber = BigInteger(
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        16
    ).toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
    return this == max256BitsNumber
}
