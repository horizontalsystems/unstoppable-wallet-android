package io.horizontalsystems.stellarkit

sealed interface StellarWallet {
    data class WatchOnly(val addressStr: String) : StellarWallet
    data class Seed(val seed: ByteArray) : StellarWallet
}