package io.horizontalsystems.walletkit.modules.walletconnect

// Supported Solana mainnet-beta CAIP-2 references: the canonical genesis-hash value and the legacy
// CAIP-30 value some dApps still use. Any other reference (devnet/testnet) is unsupported.
internal val solanaMainnetReferences = setOf(
    "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp",
    "4sGjMW1sUnHzSxGspuhpqLDx6wiyjNtZ",
)
