package io.horizontalsystems.walletkit.modules.walletconnect.solana

import io.horizontalsystems.bitcoincore.crypto.Base58

object WCSolanaHelper {

    // The Solana namespace as defined by CAIP-2 / WalletConnect.
    const val chainNamespace = "solana"

    // Mainnet-beta CAIP-2 reference (truncate(genesisHash, 32)). `canonicalMainnetChain`
    // is what current Reown docs use; `legacyMainnetChain` is the older CAIP-30 test-case
    // value that some dApps in the wild still request, so we accept both.
    const val canonicalMainnetChain = "solana:5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
    const val legacyMainnetChain = "solana:4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ"

    // Solana defines no standard session events.
    val supportedChains = listOf(canonicalMainnetChain, legacyMainnetChain)
    val supportedMethods = listOf(
        "solana_signMessage",
        "solana_signTransaction",
        "solana_signAllTransactions",
        "solana_signAndSendTransaction",
    )
    val supportedEvents = listOf<String>()

    class NoSignerException : Exception("No Solana signer available for the active account")

    fun base58Encode(bytes: ByteArray): String = Base58.encode(bytes)

    fun base58Decode(value: String): ByteArray = Base58.decode(value)
}
