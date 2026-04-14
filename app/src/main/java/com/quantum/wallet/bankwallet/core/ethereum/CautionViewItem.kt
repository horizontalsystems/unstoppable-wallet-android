package com.quantum.wallet.bankwallet.core.ethereum

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.ICaution
import com.quantum.wallet.bankwallet.core.providers.Translator

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
