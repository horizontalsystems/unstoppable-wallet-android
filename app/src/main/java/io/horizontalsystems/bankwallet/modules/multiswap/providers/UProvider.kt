package io.horizontalsystems.bankwallet.modules.multiswap.providers

import io.horizontalsystems.bankwallet.R

enum class UProvider(
    val id: String,
    val title: String,
    val icon: Int,
    val type: Type,
) {
    Near("NEAR", "Near", R.drawable.swap_provider_near, Type.DEX),
    QuickEx("QUICKEX", "QuickEx", R.drawable.swap_provider_quickex, Type.P2P),
    LetsExchange("LETSEXCHANGE", "Let's Exchange", R.drawable.swap_provider_letsexchange, Type.P2P),
    StealthEx("STEALTHEX", "StealthEX", R.drawable.swap_provider_stealthex, Type.P2P),
    Swapuz("SWAPUZ", "Swapuz", R.drawable.swap_provider_swapuz, Type.P2P);

    enum class Type {
        DEX,
        P2P
    }
}