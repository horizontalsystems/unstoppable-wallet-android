package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

enum class UProvider(
    val id: String,
    val title: String,
    val icon: Int,
    val type: SwapProviderType,
    val aml: Boolean,
) {
    Near("NEAR", "Near", R.drawable.swap_provider_near, SwapProviderType.DEX, false),
    QuickEx("QUICKEX", "QuickEx", R.drawable.swap_provider_quickex, SwapProviderType.P2P, true),
    LetsExchange("LETSEXCHANGE", "Let's Exchange", R.drawable.swap_provider_letsexchange, SwapProviderType.P2P, true),
    StealthEx("STEALTHEX", "StealthEX", R.drawable.swap_provider_stealthex, SwapProviderType.P2P, true),
    Swapuz("SWAPUZ", "Swapuz", R.drawable.swap_provider_swapuz, SwapProviderType.P2P, false);
}