package cash.p.terminal.modules.address

import android.os.Parcelable
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AddressValidationException : Exception(), Parcelable {
    class Blank : AddressValidationException()
    class Unsupported : AddressValidationException()
    class Invalid(override val cause: Throwable) : AddressValidationException()

    override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
}
