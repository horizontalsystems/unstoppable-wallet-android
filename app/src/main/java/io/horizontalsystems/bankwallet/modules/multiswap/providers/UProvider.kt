package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

enum class UProvider(
    val id: String,
    val title: String,
    val icon: Int,
    val type: SwapProviderType,
    val aml: Boolean,
    val amlPrecheck: Boolean,
    val requireTerms: Boolean,
    val riskLevel: RiskLevel,
    val isEvm: Boolean,
) {
    Near(
        "NEAR",
        "Near",
        R.drawable.swap_provider_near,
        SwapProviderType.DEX,
        false,
        false,
        true,
        RiskLevel.CONTROLLED,
        false
    ),
    QuickEx(
        "QUICKEX",
        "QuickEx",
        R.drawable.swap_provider_quickex,
        SwapProviderType.CEX,
        true,
        true,
        true,
        RiskLevel.PRECHECK,
        false
    ),
    LetsExchange(
        "LETSEXCHANGE",
        "LetsExchange",
        R.drawable.swap_provider_letsexchange,
        SwapProviderType.CEX,
        true,
        false,
        true,
        RiskLevel.CONTROLLED,
        false
    ),
    StealthEx(
        "STEALTHEX",
        "StealthEX",
        R.drawable.swap_provider_stealthex,
        SwapProviderType.CEX,
        true,
        false,
        true,
        RiskLevel.CONTROLLED,
        false
    ),
    Exolix(
        "EXOLIX",
        "Exolix",
        R.drawable.swap_provider_exolix,
        SwapProviderType.CEX,
        true,
        false,
        true,
        RiskLevel.CONTROLLED,
        false
    ),
    Swapuz(
        "SWAPUZ",
        "Swapuz",
        R.drawable.swap_provider_swapuz,
        SwapProviderType.CEX,
        false,
        false,
        true,
        RiskLevel.LIMITED,
        false
    ),
    Barter(
        "BARTER",
        "Barter",
        R.drawable.swap_provider_barter,
        SwapProviderType.DEX,
        true,
        false,
        true,
        RiskLevel.CONTROLLED,
        true
    );
}