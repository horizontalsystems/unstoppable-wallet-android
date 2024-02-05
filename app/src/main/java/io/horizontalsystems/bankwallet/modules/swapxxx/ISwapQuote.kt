package cash.p.terminal.modules.swapxxx

import cash.p.terminal.modules.send.SendModule
import cash.p.terminal.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

interface ISwapQuote {
    fun getSettingFields() : List<SwapSettingField>

    val amountOut: BigDecimal
    val fields: List<SwapDataField>
    val fee: SendModule.AmountData?
}

class SwapQuoteUniswap(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
) : ISwapQuote {
    override fun getSettingFields(): List<SwapSettingField> {
        TODO("Not yet implemented")
    }
}

class SwapQuoteUniswapV3(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    private val blockchainType: BlockchainType,
) : ISwapQuote {
    override fun getSettingFields(): List<SwapSettingField> {
        return listOf(SwapSettingFieldRecipient(blockchainType))
    }
}

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    private val blockchainType: BlockchainType,
) : ISwapQuote {
    override fun getSettingFields(): List<SwapSettingField> {
        return listOf(SwapSettingFieldRecipient(blockchainType))
    }
}