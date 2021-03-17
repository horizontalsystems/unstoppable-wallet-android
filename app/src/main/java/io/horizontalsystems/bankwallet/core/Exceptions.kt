package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.AddressValidator

class UnsupportedAccountException : Exception()
class WrongAccountTypeForThisProvider : Exception()
class LocalizedException(val errorTextRes: Int) : Exception()
class AdapterErrorWrongParameters(override val message: String) : Exception()
class EthereumKitNotCreated() : Exception()
class NoFeeSendTransactionError() : Exception()

sealed class EvmError(message: String? = null) : Throwable(message) {
    object InsufficientBalanceWithFee : EvmError()
    class ExecutionReverted(message: String?) : EvmError(message)
    class RpcError(message: String?) : EvmError(message)
}

sealed class EvmAddressError : Throwable() {
    object InvalidAddress : EvmAddressError() {
        override fun getLocalizedMessage(): String {
            return App.instance.localizedContext().getString(R.string.SwapSettings_Error_InvalidAddress)
        }
    }
}

val Throwable.convertedError: Throwable
    get() = when (this) {
        is JsonRpc.ResponseError.RpcError -> {
            if (error.message.contains("insufficient funds for transfer") || error.message.contains("gas required exceeds allowance")) {
                EvmError.InsufficientBalanceWithFee
            } else if (error.message.contains("execution reverted")) {
                EvmError.ExecutionReverted(error.message)
            } else {
                EvmError.RpcError(error.message)
            }
        }
        is AddressValidator.AddressValidationException -> {
            EvmAddressError.InvalidAddress
        }
        else -> this
    }
