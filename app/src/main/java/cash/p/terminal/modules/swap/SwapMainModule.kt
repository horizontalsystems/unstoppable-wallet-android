package cash.p.terminal.modules.swap

import android.os.Parcelable
import cash.p.terminal.entities.CurrencyValue
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue

object SwapMainModule {

    @Parcelize
    data class ApproveData(
        val blockchainType: BlockchainType,
        val token: Token,
        val spenderAddress: String,
        val amount: BigDecimal,
        val allowance: BigDecimal
    ) : Parcelable

    @Parcelize
    enum class PriceImpactLevel : Parcelable {
        Negligible, Normal, Warning, Forbidden
    }

    sealed class SwapError : Throwable() {
        object InsufficientBalanceFrom : SwapError()
    }

    @Parcelize
    data class CoinBalanceItem(
        val token: Token,
        val balance: BigDecimal?,
        val fiatBalanceValue: CurrencyValue?,
    ) : Parcelable
}

fun BigDecimal.scaleUp(scale: Int): BigInteger {
    val exponent = scale - scale()

    return if (exponent >= 0) {
        unscaledValue() * BigInteger.TEN.pow(exponent)
    } else {
        unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
    }
}
