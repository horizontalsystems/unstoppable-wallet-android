package io.horizontalsystems.walletkit.modules.main

/** Deeplinks produced by the market widget; they lead to read-only Market pages. */
object MarketDeepLinks {
    private val paths = listOf("coin-page", "top-platforms")

    fun isMarketDeepLink(deeplink: String, scheme: String): Boolean {
        if (!deeplink.startsWith("$scheme:")) return false
        val path = deeplink.removePrefix("$scheme:").trimStart('/').substringBefore('?')
        return path in paths
    }
}
