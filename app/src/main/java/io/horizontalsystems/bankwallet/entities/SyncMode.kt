package io.horizontalsystems.bankwallet.entities

enum class SyncMode(val value: String) {
    FAST("fast"),
    SLOW("slow");

    companion object {
        private val map = values().associateBy(SyncMode::value)
        fun fromString(type: String) = map[type]
    }
}
