package io.horizontalsystems.walletkit.modules.address

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator

sealed class AddressValidationException : Exception() {
    object Blank : AddressValidationException()
    class Unsupported(val blockchain: String? = null) : AddressValidationException()
    class Invalid(override val cause: Throwable, val blockchain: String? = null) : AddressValidationException()

    private val blockchain: String?
        get() = when (this) {
            is Blank -> null
            is Invalid -> blockchain
            is Unsupported -> blockchain
        }

    override fun getLocalizedMessage() = when (val blockchainName = blockchain) {
        null -> Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
        else -> Translator.getString(R.string.SwapSettings_Error_InvalidBlockchainAddress, blockchainName)
    }
}
