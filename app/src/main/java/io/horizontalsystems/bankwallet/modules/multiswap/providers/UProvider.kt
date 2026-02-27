package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

enum class UProvider(
    val id: String,
    val title: String,
    val icon: Int,
    val type: SwapProviderType,
    val aml: Boolean,
    val requireTerms: Boolean,
    val riskLevel: RiskLevel,
) {
    Near(
        "NEAR",
        "Near",
        R.drawable.swap_provider_near,
        SwapProviderType.DEX,
        false,
        false,
        RiskLevel.CONTROLLED
    ),
    QuickEx(
        "QUICKEX",
        "QuickEx",
        R.drawable.swap_provider_quickex,
        SwapProviderType.CEX,
        true,
        true,
        RiskLevel.CONTROLLED
    ),
    LetsExchange(
        "LETSEXCHANGE",
        "LetsExchange",
        R.drawable.swap_provider_letsexchange,
        SwapProviderType.CEX,
        true,
        true,
        RiskLevel.CONTROLLED
    ),
    StealthEx(
        "STEALTHEX",
        "StealthEX",
        R.drawable.swap_provider_stealthex,
        SwapProviderType.CEX,
        true,
        true,
        RiskLevel.CONTROLLED
    ),
    Swapuz(
        "SWAPUZ",
        "Swapuz",
        R.drawable.swap_provider_swapuz,
        SwapProviderType.CEX,
        false,
        false,
        RiskLevel.LIMITED
    );
}