package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bitcoincore.BitcoinCore

enum class SyncMode(val value: String) {
    FAST("fast"),
    SLOW("slow"),
    NEW("new");

    fun bitcoinKitMode(): BitcoinCore.SyncMode{
        return when(this) {
            FAST -> BitcoinCore.SyncMode.Api()
            SLOW -> BitcoinCore.SyncMode.Full()
            NEW -> BitcoinCore.SyncMode.NewWallet()
        }
    }

    companion object {
        private val map = values().associateBy(SyncMode::value)
        fun fromString(type: String) = map[type]
    }
}
