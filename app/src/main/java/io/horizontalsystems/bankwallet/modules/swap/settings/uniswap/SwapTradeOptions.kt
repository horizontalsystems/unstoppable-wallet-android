package io.horizontalsystems.bankwallet.modules.swap.settings.uniswap

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.uniswapkit.models.TradeOptions
import java.math.BigDecimal

class SwapTradeOptions(
    var allowedSlippage: BigDecimal = TradeOptions.defaultAllowedSlippage,
    var ttl: Long = TradeOptions.defaultTtl,
    var recipient: Address? = null
) {

    val tradeOptions: TradeOptions
        get() {
            val address = recipient?.let {
                try {
                    io.horizontalsystems.ethereumkit.models.Address(it.hex)
                } catch (err: Exception) {
                    null
                }
            }

            return TradeOptions(allowedSlippage, ttl, address)
        }
}
