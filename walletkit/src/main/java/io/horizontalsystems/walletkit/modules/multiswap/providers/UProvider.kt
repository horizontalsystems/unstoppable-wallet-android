package io.horizontalsystems.walletkit.modules.multiswap.providers

enum class UProvider(
    val id: String,
    val title: String,
    val type: SwapProviderType,
    val aml: Boolean,
    val amlPrecheck: Boolean,
    val requireTerms: Boolean,
    val riskLevel: RiskLevel,
    val isEvm: Boolean,
    val isSingleTransactionSwap: Boolean,
    // Provider accepts plain sends to its deposit address (any output type) on UTXO
    // chains (Bitcoin-family, Zcash, Monero, Zano). Providers with special tx
    // requirements (e.g. thorchain memos) must be false and handled separately if ever needed.
    val supportsSimpleUtxoTransactions: Boolean,
) {
    Near(
        "NEAR",
        "Near",
        SwapProviderType.DEX,
        false,
        false,
        true,
        RiskLevel.FAIR,
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
        RiskLevel.GOOD,
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
        RiskLevel.GOOD,
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
        RiskLevel.FAIR,
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
        RiskLevel.GOOD,
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
        RiskLevel.GOOD,
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
        RiskLevel.GOOD,
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
        RiskLevel.GOOD,
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
        RiskLevel.GOOD,
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
        RiskLevel.FAIR,
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
        RiskLevel.EXCELLENT,
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
        riskLevel = RiskLevel.GOOD,
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
        riskLevel = RiskLevel.EXCELLENT,
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
        riskLevel = RiskLevel.EXCELLENT,
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
        riskLevel = RiskLevel.EXCELLENT,
        isEvm = false,
        // Cross-chain: the signed inbound tx is followed by Axelar's GMP delivery leg.
        isSingleTransactionSwap = false,
        supportsSimpleUtxoTransactions = false
    );
}
