package io.horizontalsystems.walletkit.modules.walletconnect.session

import java.net.URI

/**
 * Decides whether a dApp URL belongs to the WalletConnect whitelist.
 *
 * Matching is done on the parsed host only. Comparing whole URL strings lets an untrusted
 * origin inherit trust in two ways: through a lookalike registrable domain, since
 * "eviluniswap.org" ends with "uniswap.org", and through a crafted path, since
 * "evil.com/uniswap.org" also ends with "uniswap.org". Both are attacker-controlled, so the
 * host is extracted before any comparison and only an exact host or a real subdomain matches.
 */
object WCWhitelistMatcher {

    fun isHostInWhiteList(url: String, whiteListUrls: List<String>): Boolean {
        val host = hostOf(url) ?: return false

        return whiteListUrls.any { whiteListUrl ->
            val whiteListHost = hostOf(whiteListUrl) ?: return@any false

            host == whiteListHost || host.endsWith(".$whiteListHost")
        }
    }

    /**
     * Returns the normalized host, or null when it cannot be determined. Callers treat null as
     * "not whitelisted" so that anything unparseable fails closed.
     */
    fun hostOf(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        // URI needs a scheme to populate the host, and whitelist entries may be bare domains.
        val absolute = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        val host = try {
            URI(absolute).host
        } catch (e: Exception) {
            null
        } ?: return null

        // Case and the root's trailing dot carry no meaning, but every label does: stripping "www."
        // would let a whitelisted "www.example.com" stand in for the apex and all of its other
        // subdomains. A dApp reporting "www.example.com" still matches a whitelisted
        // "example.com" through the subdomain rule.
        return host.lowercase()
            .removeSuffix(".")
            .takeIf { it.isNotEmpty() }
    }
}
