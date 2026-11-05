package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.walletkit.modules.multiswap.action.ISwapProviderAction
import io.horizontalsystems.walletkit.modules.multiswap.providers.SwapVenue
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SwapQuote(
    val amountOut: BigDecimal,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigDecimal,
    val actionRequired: ISwapProviderAction?,
    val estimationTime: Long?,
    // The venue the route is served by: the card's identity on the screen and the provider the
    // preview and the swap record name.
    val venue: SwapVenue,
    // Present when the route has an AML address precheck to run before the deposit is sent.
    val amlProviderId: String? = null,
    val extraData: ExtraData? = null,
) {
    interface ExtraData
}
