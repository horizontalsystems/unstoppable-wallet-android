package io.horizontalsystems.bankwallet.core.ethereum

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ICaution
import io.horizontalsystems.bankwallet.core.providers.Translator

data class CautionViewItem(val title: String, val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }

    companion object {
        fun fromThrowable(error: Throwable) = when (error) {
            is ICaution -> error.toCautionViewItem()

            else -> CautionViewItem(
                title = Translator.getString(R.string.Error),
                text = error.message ?: error.javaClass.simpleName,
                type = Type.Error
            )
        }
    }
}
