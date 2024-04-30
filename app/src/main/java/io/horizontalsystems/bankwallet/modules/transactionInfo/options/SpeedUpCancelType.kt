package io.horizontalsystems.bankwallet.modules.transactionInfo.options

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SpeedUpCancelType : Parcelable {
    SpeedUp, Cancel
}
