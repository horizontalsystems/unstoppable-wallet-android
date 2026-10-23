package io.horizontalsystems.walletkit.modules.multiswap.providers

enum class UProvider(
    val id: String,
    val title: String,
    val type: SwapProviderType,
    val aml: Boolean,
    val amlPrecheck: Boolean,
    val requireTerms: Boolean,
    val isEvm: Boolean,
    val isSingleTransactionSwap: Boolean,
    // Provider accepts plain sends to its deposit address (any output type) on UTXO
    // chains (Bitcoin-family, Zcash, Monero, Zano). Providers with special tx
    // requirements (e.g. thorchain memos) must be false and handled separately if ever needed.
    val supportsSimpleUtxoTransactions: Boolean,
    // Takes a unified ZEC address as the refund target of a ZEC sell. App data: /v3/providers
    // does not publish which ZEC address types a provider accepts. Transfer venues validate a
    // refund as transparent (QuickEx and StealthEX reject `u1`), Maya refunds to the address in
    // the memo (Sapling receiver), NEAR's 1Click to an Orchard one. A provider with the flag also
    // gets the refund address as `sourceAddress`, and the refund itself is the shielded-only
    // unified address (see ZcashSellAddresses).
    val acceptsUnifiedZcashRefund: Boolean = false,
) {
    Near(
        "NEAR",
        "Near",
        SwapProviderType.DEX,
        false,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true,
        acceptsUnifiedZcashRefund = true,
    ),
    // NEAR's confidential rail: the swap executes on a private shard, so the payout cannot be
    // linked on-chain to the deposit. Quoted on the swap screen like any other provider and
    // also the rail behind private send (see MultiSwapProviderRegistry.confidentialProviders).
    // Refunds a ZEC sell to the transparent address: whether its payout bridge pays a unified
    // address without a transparent receiver is unverified.
    NearConfidential(
        "NEAR_CONFIDENTIAL",
        "Near Confidential",
        SwapProviderType.DEX,
        false,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    QuickEx(
        "QUICKEX",
        "QuickEx",
        SwapProviderType.CEX,
        true,
        true,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    LetsExchange(
        "LETSEXCHANGE",
        "LetsExchange",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    StealthEx(
        "STEALTHEX",
        "StealthEX",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Exolix(
        "EXOLIX",
        "Exolix",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Cce(
        "CCE",
        "CCE Cash",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Swapuz(
        "SWAPUZ",
        "Swapuz",
        SwapProviderType.CEX,
        false,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Lizex(
        "LIZEX",
        "Lizex",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Bitania(
        "BITANIA",
        "Bitania",
        SwapProviderType.CEX,
        true,
        false,
        true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    XSwap(
        id = "XSWAP",
        title = "XSwap",
        type = SwapProviderType.CEX,
        aml = true,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Barter(
        "BARTER",
        "Barter",
        SwapProviderType.DEX,
        true,
        false,
        true,
        isEvm = true,
        isSingleTransactionSwap = true,
        supportsSimpleUtxoTransactions = false
    ),
    Circle(
        "CIRCLE",
        "Circle CCTP",
        SwapProviderType.DEX,
        false,
        false,
        true,
        isEvm = true,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false
    ),
    Pegasus(
        id = "PEGASUS",
        title = "PegasusSwap",
        type = SwapProviderType.CEX,
        aml = true,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = true
    ),
    Jupiter(
        id = "JUPITER",
        title = "Jupiter",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        isSingleTransactionSwap = true,
        supportsSimpleUtxoTransactions = false
    ),
    Lifi(
        id = "LIFI",
        title = "LI.FI",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        // Cross-chain default; a same-chain LI.FI pair IS a single tx —
        // USwapProvider.isSingleTransactionSwap resolves that per pair.
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false
    ),
    // Same-token 1:1 bridge (not a swap) for XLM and classic SHX between Stellar and
    // Ethereum via Axelar's Interchain Token Service. Both legs are server-built signed
    // transactions (EVM contract call / Stellar XDR envelope).
    AxelarIts(
        id = "AXELAR_ITS",
        title = "Axelar ITS",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        // Cross-chain: the signed inbound tx is followed by Axelar's GMP delivery leg.
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false
    ),
    // Routes execute as thorchain_deposit: a vault deposit carrying the swap memo. UTXO sells
    // need the vault's input and change rules rather than a plain send (see USwapProvider).
    ThorChain(
        id = "THORCHAIN",
        title = "THORChain",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = false,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false
    ),
    Maya(
        id = "MAYACHAIN",
        title = "Maya Protocol",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = false,
        isEvm = false,
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false,
        acceptsUnifiedZcashRefund = true,
    ),
    // Same-chain EVM aggregator routes as server-built signed transactions. No token list:
    // assets are the raw contract addresses on the sell token's chain.
    OneInch(
        id = "ONEINCH",
        title = "1inch",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = false,
        isEvm = true,
        isSingleTransactionSwap = true,
        supportsSimpleUtxoTransactions = false
    ),
    Uniswap(
        id = "UNISWAP",
        title = "Uniswap",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = false,
        isEvm = true,
        isSingleTransactionSwap = true,
        supportsSimpleUtxoTransactions = false
    ),
    // One card for the Stellar-native DEX routes: /v3/rate fans out over SOROSWAP, AQUARIUS
    // and STELLAR_DEX and the best-priced route is previewed with that provider. The id is
    // the server's STELLARBROKER entry, which swap records made through this card carry.
    Stellar(
        id = "STELLARBROKER",
        title = "StellarBroker",
        type = SwapProviderType.DEX,
        aml = false,
        amlPrecheck = false,
        requireTerms = true,
        isEvm = false,
        isSingleTransactionSwap = true,
        supportsSimpleUtxoTransactions = false
    );
}
