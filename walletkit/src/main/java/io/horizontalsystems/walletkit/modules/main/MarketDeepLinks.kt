package io.horizontalsystems.walletkit.modules.main

import android.net.Uri
import io.horizontalsystems.walletkit.modules.coin.CoinPage
import io.horizontalsystems.walletkit.modules.market.platform.MarketPlatformPage
import io.horizontalsystems.walletkit.modules.market.topplatforms.Platform
import io.horizontalsystems.walletkit.modules.nav3.HSPage

/** Deeplinks produced by the market widget; they lead to read-only Market pages. */
object MarketDeepLinks {
    private const val COIN_PAGE = "coin-page"
    private const val TOP_PLATFORMS = "top-platforms"
    private val paths = listOf(COIN_PAGE, TOP_PLATFORMS)

    fun isMarketDeepLink(deeplink: String, scheme: String): Boolean =
        path(deeplink, scheme) in paths

    /** Page a market deeplink opens, or null when it is not one or lacks its parameters. */
    fun page(uri: Uri, scheme: String): HSPage? {
        // The path is checked before any query parameter is read: a payment link such as
        // `bitcoin:<address>?amount=` is not hierarchical, and asking it for a parameter throws.
        val path = path(uri.toString(), scheme)?.takeIf { it in paths } ?: return null
        val uid = uri.getQueryParameter("uid") ?: return null
        return when (path) {
            COIN_PAGE -> CoinPage(CoinPage.Input(uid))
            TOP_PLATFORMS -> uri.getQueryParameter("title")?.let { MarketPlatformPage(Platform(uid, it)) }
            else -> null
        }
    }

    private fun path(deeplink: String, scheme: String): String? {
        if (!deeplink.startsWith("$scheme:")) return null
        return deeplink.removePrefix("$scheme:").trimStart('/').substringBefore('?')
    }
}
