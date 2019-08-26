package io.horizontalsystems.bankwallet.modules.send.submodules.confirmation

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class SendConfirmationInfo(
        val primaryAmount: String,
        val secondaryAmount: String?,
        val receiver: String,
        val fee: String?,
        val total: String?,
        val duration: String?,
        val showMemo: Boolean = false): Parcelable {
}