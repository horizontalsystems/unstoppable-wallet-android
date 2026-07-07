package io.horizontalsystems.walletkit.entities


enum class SyncMode(val value: String) {
    Fast("Fast"),
    Slow("Slow"),
    New("New");

    val title: String
        get() = when (this) {
            New -> "API"
            Fast -> "API"
            Slow -> "Blockchain"
        }
}
