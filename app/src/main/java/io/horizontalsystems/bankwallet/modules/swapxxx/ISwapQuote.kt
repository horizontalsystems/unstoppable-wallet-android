package io.horizontalsystems.bankwallet.modules.swapxxx

import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.swapxxx.settings.ISwapSettingField
import io.horizontalsystems.bankwallet.modules.swapxxx.ui.SwapDataField
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

interface ISwapQuote {
    fun getSettingFields() : List<ISwapSettingField>

    val amountOut: BigDecimal
    val fields: List<SwapDataField>
    val fee: SendModule.AmountData?
}

class SwapQuoteUniswap(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    private val settingFields: List<ISwapSettingField>,
) : ISwapQuote {
    override fun getSettingFields() = settingFields
}

class SwapQuoteUniswapV3(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    private val blockchainType: BlockchainType,
) : ISwapQuote {
    override fun getSettingFields(): List<ISwapSettingField> {
        TODO()
//        return listOf(SwapSettingFieldRecipient(blockchainType))
    }
}

class SwapQuoteOneInch(
    override val amountOut: BigDecimal,
    override val fields: List<SwapDataField>,
    override val fee: SendModule.AmountData?,
    private val blockchainType: BlockchainType,
) : ISwapQuote {
    override fun getSettingFields(): List<ISwapSettingField> {
        TODO()
//        return listOf(SwapSettingFieldRecipient(blockchainType))
    }
}