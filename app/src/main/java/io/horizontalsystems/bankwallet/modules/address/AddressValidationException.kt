package io.horizontalsystems.bankwallet.modules.address

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class AddressValidationException : Exception(), Parcelable {
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
