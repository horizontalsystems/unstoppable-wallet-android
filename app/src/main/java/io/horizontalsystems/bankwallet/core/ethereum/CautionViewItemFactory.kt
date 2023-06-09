package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.EvmError
import io.horizontalsystems.bankwallet.core.Warning
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsError
import io.horizontalsystems.bankwallet.modules.evmfee.FeeSettingsWarning
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.UniswapWarnings

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
            FeeSettingsWarning.RiskOfGettingStuckLegacy -> {
                CautionViewItem(
                    Translator.getString(R.string.FeeSettings_RiskOfGettingStuckLegacy_Title),
                    Translator.getString(R.string.FeeSettings_RiskOfGettingStuckLegacy),
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
            is UniswapWarnings.PriceImpactForbidden -> {
                CautionViewItem(
                    Translator.getString(R.string.Swap_PriceImpact),
                    Translator.getString(R.string.Swap_PriceImpactTooHigh, warning.providerName),
                    CautionViewItem.Type.Error
                )
            }
            UniswapWarnings.PriceImpactWarning -> {
                CautionViewItem(
                    Translator.getString(R.string.Swap_PriceImpact),
                    Translator.getString(R.string.Swap_PriceImpactWarning),
                    CautionViewItem.Type.Error
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
                        baseCoinService.token.coin.code
                    ),
                    CautionViewItem.Type.Error
                )
            }
            FeeSettingsError.UsedNonce -> {
                CautionViewItem(
                    Translator.getString(R.string.SendEvmSettings_Error_NonceUsed_Title),
                    Translator.getString(R.string.SendEvmSettings_Error_NonceUsed),
                    CautionViewItem.Type.Error
                )
            }
            else -> {
                val (title, text) = convertError(error)
                CautionViewItem(title, text, CautionViewItem.Type.Error)
            }
        }
    }

    private fun convertError(error: Throwable): Pair<String, String> =
        when (val convertedError = error.convertedError) {
            is SendEvmTransactionService.TransactionError.InsufficientBalance -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_Title),
                    Translator.getString(
                        R.string.EthereumTransaction_Error_InsufficientBalance,
                        baseCoinService.coinValue(convertedError.requiredBalance).getFormattedFull()
                    )
                )
            }
            is EvmError.InsufficientBalanceWithFee -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_Title),
                    Translator.getString(
                        R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
                        baseCoinService.token.coin.code
                    )
                )
            }
            is EvmError.ExecutionReverted -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_Title),
                    Translator.getString(
                        R.string.EthereumTransaction_Error_ExecutionReverted,
                        convertedError.message ?: ""
                    )
                )
            }
            is EvmError.CannotEstimateSwap -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_CannotEstimate_Title),
                    Translator.getString(
                        R.string.EthereumTransaction_Error_CannotEstimate,
                        baseCoinService.token.coin.code
                    )
                )
            }
            is EvmError.LowerThanBaseGasLimit -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_LowerThanBaseGasLimit_Title),
                    Translator.getString(R.string.EthereumTransaction_Error_LowerThanBaseGasLimit)
                )
            }
            is EvmError.InsufficientLiquidity -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity_Title),
                    Translator.getString(R.string.EthereumTransaction_Error_InsufficientLiquidity)
                )
            }
            else -> {
                Pair(
                    Translator.getString(R.string.EthereumTransaction_Error_Title),
                    convertedError.message ?: convertedError.javaClass.simpleName
                )
            }
        }
}
