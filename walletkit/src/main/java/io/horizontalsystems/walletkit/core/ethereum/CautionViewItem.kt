package io.horizontalsystems.walletkit.core.ethereum

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.ICaution
import io.horizontalsystems.walletkit.core.providers.Translator

data class CautionViewItem(val title: String, val text: String, val type: Type) {
    enum class Type {
        Error, Warning
    }

    companion object {
        /** The balance of [coinCode] cannot cover a transfer; used where no send service estimates it. */
        fun insufficientBalance(coinCode: String) = CautionViewItem(
            title = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalance_Title),
            text = Translator.getString(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, coinCode),
            type = Type.Error
        )

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
