package io.horizontalsystems.bankwallet.modules.sendevm

import android.os.Parcelable
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.android.parcel.Parcelize


data class SendEvmData(
        val transactionData: TransactionData,
        val additionalItems: List<AdditionalItem> = listOf()
) {
    sealed class AdditionalItem : Parcelable {
        @Parcelize
        class Domain(val value: String) : AdditionalItem()
    }
}

object SendEvmModule {


}
