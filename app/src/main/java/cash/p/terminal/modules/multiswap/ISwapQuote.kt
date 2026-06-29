package cash.p.terminal.modules.multiswap

import cash.p.terminal.core.HSCaution
import cash.p.terminal.modules.multiswap.action.ISwapProviderAction
import cash.p.terminal.modules.multiswap.settings.ISwapSetting
import cash.p.terminal.modules.multiswap.ui.DataField
import cash.p.terminal.wallet.Token
import io.horizontalsystems.uniswapkit.models.TradeData
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigDecimal

interface ISwapQuote {
    val amountOut: BigDecimal
    val priceImpact: BigDecimal?
    val fields: List<DataField>
    val settings: List<ISwapSetting>
    val tokenIn: Token
    val tokenOut: Token
    val amountIn: BigDecimal
    val actionRequired: ISwapProviderAction?
    val cautions: List<HSCaution>

    // Estimated execution time in seconds, or null when the provider exposes none (the UI then
    // hides the ETA badge). Abstract on purpose: every quote must state its ETA explicitly, so a
    // new provider can't silently inherit a default and forget to set it.
    val estimationTime: Long?
}

class SwapQuoteUniswap(
    val tradeData: TradeData,
    override val fields: List<DataField>,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val actionRequired: ISwapProviderAction?,
    override val cautions: List<HSCaution> = listOf()
) : ISwapQuote {
    override val amountOut: BigDecimal = tradeData.amountOut!!
    override val priceImpact: BigDecimal? = tradeData.priceImpact
    override val estimationTime: Long? = null
}

class SwapQuoteUniswapV3(
    val tradeDataV3: TradeDataV3,
    override val fields: List<DataField>,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val actionRequired: ISwapProviderAction?,
    override val cautions: List<HSCaution> = listOf()
) : ISwapQuote {
    override val amountOut = tradeDataV3.tokenAmountOut.decimalAmount!!
    override val priceImpact = tradeDataV3.priceImpact
    override val estimationTime: Long? = null
}

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val actionRequired: ISwapProviderAction?,
    override val cautions: List<HSCaution> = listOf()
) : ISwapQuote {
    override val estimationTime: Long? = null
}

class SwapQuoteOffChain(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val actionRequired: ISwapProviderAction?,
    override val cautions: List<HSCaution> = listOf(),
    override val estimationTime: Long? = null,
) : ISwapQuote

class SwapQuoteThorChain(
    override val amountOut: BigDecimal,
    override val priceImpact: BigDecimal?,
    override val fields: List<DataField>,
    override val settings: List<ISwapSetting>,
    override val tokenIn: Token,
    override val tokenOut: Token,
    override val amountIn: BigDecimal,
    override val actionRequired: ISwapProviderAction?,
    override val cautions: List<HSCaution>,
    val slippageThreshold: BigDecimal,
    override val estimationTime: Long? = null,
) : ISwapQuote
