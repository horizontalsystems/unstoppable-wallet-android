package io.horizontalsystems.walletkit.modules.zcashnetwork.addendpoint

import java.net.URI

internal object ZcashEndpointUrl {

    /**
     * Turns what the user typed into the endpoint url to store, or null when it is not a usable
     * https url. An input without a scheme, such as `host:port`, is read as https: parsed as it
     * stands, [URI] would take the host for the scheme. A user info part is refused: the client
     * connects by host and port only, and `mailto:user@host` would otherwise pass as a host. For
     * the same reason a path, query or fragment is refused: it would be stored but never used, so
     * a server behind `host/path` would not be the one reached.
     */
    fun parse(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val url = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        return try {
            val uri = URI(url)
            val isHttps = uri.scheme?.lowercase() == "https"
            val hasHost = !uri.host.isNullOrBlank()
            val portIsValid = uri.port == -1 || uri.port in 1..65535
            val isHostOnly = uri.userInfo == null && uri.rawPath.orEmpty() in listOf("", "/") &&
                uri.rawQuery == null && uri.rawFragment == null
            url.takeIf { isHttps && hasHost && portIsValid && isHostOnly }
        } catch (_: Exception) {
            null
        }
    }
}
