package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bitcoincore.BitcoinCore
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class SyncMode(val value: String) : Parcelable {
    FAST("fast"),
    SLOW("slow"),
    NEW("new");

    companion object {
        private val map = values().associateBy(SyncMode::value)
        fun fromString(type: String) = map[type] ?: FAST
        fun fromSyncMode(mode: SyncMode): BitcoinCore.SyncMode {
            return when (mode) {
                FAST -> BitcoinCore.SyncMode.Api()
                SLOW -> BitcoinCore.SyncMode.Full()
                NEW -> BitcoinCore.SyncMode.NewWallet()
            }
        }
    }
}
