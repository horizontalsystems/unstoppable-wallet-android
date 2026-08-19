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

    // Resource-exhaustion guards for untrusted dApp requests. A malicious connected dApp must not be
    // able to make the wallet decode/sign an unbounded amount of data (CPU/heap DoS, OOM crash), so
    // requests are size- and count-bounded BEFORE any base64/base58 decode or signing.
    //
    // A Solana transaction is at most 1232 bytes (packet MTU), i.e. ~1644 base64 chars; cap with
    // headroom and reject anything larger.
    const val maxTransactionBase64Length = 2048
    // solana_signAllTransactions batches are small in practice; bound them so one request can't drive
    // unbounded decode/sign work.
    const val maxTransactionsPerRequest = 32
    // Upper bound on the raw params JSON, so a giant array can't exhaust the heap during JSON parsing
    // before the per-item caps apply (maxTransactionsPerRequest * maxTransactionBase64Length + slack).
    const val maxParamsJsonLength = 128 * 1024

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
