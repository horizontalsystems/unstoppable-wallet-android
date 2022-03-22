package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator

sealed class AddressValidationException : Exception() {
    class Blank : AddressValidationException()
    class Unsupported : AddressValidationException()
    class Invalid(override val cause: Exception) : AddressValidationException()

    override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
}
