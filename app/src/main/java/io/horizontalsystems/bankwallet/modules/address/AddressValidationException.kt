package io.horizontalsystems.bankwallet.modules.address

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AddressValidationException : Exception(), Parcelable {
    class Blank : AddressValidationException()
    class Unsupported : AddressValidationException()
    class Invalid(override val cause: Throwable) : AddressValidationException()

    override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
}
