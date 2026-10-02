package io.horizontalsystems.walletkit.core.utils

import java.net.URI
import java.util.Locale

/**
 * True when [uri] is HTTP to a .onion host. Ordinary public hosts still require a
 * secure scheme; this is the Tor exception for custom EVM and Monero nodes.
 *
 * The host must be a nonempty subdomain of onion (dot boundary, case-insensitive).
 * Bare "onion", lookalikes such as "notonion" or "onion.example.com", and
 * ".onion" appearing only in userinfo, path, or query do not match.
 */
object OnionNodeUrl {

    fun isHttpOnion(uri: URI): Boolean {
        // Case-sensitive, like the https/wss check in AddRpcViewModel.
        if (uri.scheme != "http") {
            return false
        }
        val host = uri.host ?: return false
        return isOnionHost(host)
    }

    private fun isOnionHost(host: String): Boolean {
        // Locale.ROOT keeps ASCII I and İ (U+0130) off the device locale's case map.
        val normalized = host.lowercase(Locale.ROOT).removeSuffix(".")
        return normalized.endsWith(".onion") && normalized.length > ".onion".length
    }
}
