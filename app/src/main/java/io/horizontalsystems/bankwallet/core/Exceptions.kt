package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.AddressValidator

class UnsupportedException(override val message: String?) : Exception()
class UnsupportedAccountException : Exception()
class LocalizedException(val errorTextRes: Int) : Exception()
class AdapterErrorWrongParameters(override val message: String) : Exception()
class NoFeeSendTransactionError : Exception()
class FailedTransaction(errorMessage: String?) : RuntimeException(errorMessage) {
    override fun toString() = message ?: "Transaction failed."
}

sealed class EvmError(message: String? = null) : Throwable(message) {
    object InsufficientBalanceWithFee : EvmError()
    object CannotEstimateSwap : EvmError()
    object InsufficientLiquidity : EvmError()
    object LowerThanBaseGasLimit : EvmError()
    class ExecutionReverted(message: String?) : EvmError(message)
    class RpcError(message: String?) : EvmError(message)
}

sealed class EvmAddressError : Throwable() {
    object InvalidAddress : EvmAddressError() {
        override fun getLocalizedMessage(): String {
            return Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
        }
    }
}

val Throwable.convertedError: Throwable
    get() = when (this) {
        is JsonRpc.ResponseError.RpcError -> {
            if (error.message.contains("insufficient funds for transfer") || error.message.contains(
                    "gas required exceeds allowance"
                )
            ) {
                EvmError.InsufficientBalanceWithFee
            } else if (error.message.contains("max fee per gas less than block base fee")) {
                EvmError.LowerThanBaseGasLimit
            } else if (error.message.contains("execution reverted")) {
                EvmError.ExecutionReverted(error.message)
            } else {
                EvmError.RpcError(error.message)
            }
        }
        is AddressValidator.AddressValidationException -> {
            EvmAddressError.InvalidAddress
        }
        is retrofit2.HttpException -> {
            val errorBody = response()?.errorBody()?.string()
            if (errorBody?.contains("Try to leave the buffer of ETH for gas") == true ||
                errorBody?.contains("you may not have enough ETH balance for gas fee") == true ||
                errorBody?.contains("Not enough ETH balance") == true ||
                errorBody?.contains("insufficient funds for transfer") == true
            ) {
                EvmError.InsufficientBalanceWithFee
            } else if (errorBody?.contains("cannot estimate") == true) {
                EvmError.CannotEstimateSwap
            } else if (errorBody?.contains("insufficient liquidity") == true) {
                EvmError.InsufficientLiquidity
            } else {
                this
            }
        }
        else -> this
    }
