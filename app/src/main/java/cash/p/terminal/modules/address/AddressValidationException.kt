package cash.p.terminal.modules.address

import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator

sealed class AddressValidationException : Exception() {
    class Blank : AddressValidationException()
    class Unsupported : AddressValidationException()
    class Invalid(override val cause: Throwable) : AddressValidationException()

    override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
}
