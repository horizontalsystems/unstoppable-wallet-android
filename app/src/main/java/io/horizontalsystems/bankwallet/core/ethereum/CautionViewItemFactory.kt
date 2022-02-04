package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.uniswap.UniswapModule

class CautionViewItemFactory(
    private val baseCoinService: EvmCoinService
) {
    fun cautionViewItems(warnings: List<Warning>, errors: List<Throwable>): List<CautionViewItem> {
        return warnings.map { cautionViewItem(it) } + errors.map { cautionViewItem(it) }
    }

    private fun cautionViewItem(warning: Warning): CautionViewItem {
        return when (warning) {
            FeeSettingsWarning.RiskOfGettingStuck -> {
                CautionViewItem(
                    Translator.getString(R.string.FeeSettings_RiskOfGettingStuck_Title),
                    Translator.getString(R.string.FeeSettings_RiskOfGettingStuck),
                    CautionViewItem.Type.Warning
                )
            }
            FeeSettingsWarning.Overpricing -> {
                CautionViewItem(
                    Translator.getString(R.string.FeeSettings_Overpricing_Title),
                    Translator.getString(R.string.FeeSettings_Overpricing),
                    CautionViewItem.Type.Warning
                )
            }
            FeeSettingsWarning.HighBaseFeeWarning -> {
                CautionViewItem(
                    Translator.getString(R.string.FeeSettings_BaseFeeIsHigh_Title),
                    Translator.getString(R.string.FeeSettings_BaseFeeIsHigh),
                    CautionViewItem.Type.Warning
                )
            }
            UniswapModule.UniswapWarnings.PriceImpactWarning -> {
                CautionViewItem(
                    Translator.getString(R.string.Swap_PriceImpact),
                    Translator.getString(R.string.Swap_PriceImpactTooHigh),
                    CautionViewItem.Type.Warning
                )
            }
            else -> {
                CautionViewItem(
                    Translator.getString(R.string.EthereumTransaction_Warning_Title),
                    warning.javaClass.simpleName,
                    CautionViewItem.Type.Warning
                )
            }
        }
    }

    private fun cautionViewItem(error: Throwable): CautionViewItem {
        return when (error) {
            FeeSettingsError.InsufficientBalance -> {
                CautionViewItem(
                    Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
                    Translator.getString(
                        R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                        baseCoinService.platformCoin.coin.code
                    ),
                    CautionViewItem.Type.Error
                )
            }
            FeeSettingsError.LowBaseFee -> {
                CautionViewItem(
                    Translator.getString(R.string.FeeSettings_BaseFeeIsLow_Title),
                    Translator.getString(R.string.FeeSettings_BaseFeeIsLow),
                    CautionViewItem.Type.Error
                )
            }
            else -> {
                CautionViewItem(
                    Translator.getString(R.string.EthereumTransaction_Error_Title),
                    convertError(error),
                    CautionViewItem.Type.Error
                )
            }
        }
    }

    private fun convertError(error: Throwable): String =
        when (val convertedError = error.convertedError) {
            is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalance,
                    baseCoinService.coinValue(convertedError.requiredBalance)
                        .getFormatted()
                )
            }
            is EvmError.InsufficientBalanceWithFee,
            is EvmError.ExecutionReverted -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                    baseCoinService.platformCoin.code
                )
            }
            is EvmError.CannotEstimateSwap -> {
                Translator.getString(
                    R.string.EthereumTransaction_Error_CannotEstimate,
                    baseCoinService.platformCoin.code
                )
            }
            is EvmError.LowerThanBaseGasLimit -> Translator.getString(R.string.EthereumTransaction_Error_LowerThanBaseGasLimit)
            is EvmError.InsufficientLiquidity -> Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
            else -> convertedError.message ?: convertedError.javaClass.simpleName
        }
}
