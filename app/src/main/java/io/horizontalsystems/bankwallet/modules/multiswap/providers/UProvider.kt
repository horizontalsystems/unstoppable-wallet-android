package io.horizontalsystems.bankwallet.modules.multiswap.providers

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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = true
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
        isSingleTransactionSwap = false
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
        isSingleTransactionSwap = false
    );
}
