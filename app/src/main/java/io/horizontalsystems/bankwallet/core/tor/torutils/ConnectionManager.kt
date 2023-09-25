package io.horizontalsystems.bankwallet.core.tor.torutils

import java.util.logging.Logger

enum class ProxyEnvVar(val value: String) {

    USE_SYSTEM_PROXIES("java.net.useSystemProxies"),
    HTTP_PROXY_HOST("http.proxyHost"),
    HTTP_PROXY_PORT("http.proxyPort"),
    HTTP_NONPROXY_HOSTS("http.nonProxyHosts"),
    HTTPS_PROXY_HOST("https.proxyHost"),
    HTTPS_PROXY_PORT("https.proxyPort"),
    HTTPS_NONPROXY_HOSTS("https.nonProxyHosts"),
    SOCKS_PROXY_HOST("socksProxyHost"),
    SOCKS_PROXY_PORT("socksProxyPort");
}

object TorConnectionManager {

    private val logger = Logger.getLogger("ConnectionManager")

    fun setSystemProxy(userSystemProxy: Boolean, host: String, httpPort: String, socksPort: String) {

        System.setProperty(ProxyEnvVar.USE_SYSTEM_PROXIES.value, userSystemProxy.toString());
        System.setProperty(ProxyEnvVar.HTTP_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.HTTP_PROXY_PORT.value, httpPort)
        System.setProperty(ProxyEnvVar.HTTPS_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.HTTPS_PROXY_PORT.value, httpPort)
        System.setProperty(ProxyEnvVar.SOCKS_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.SOCKS_PROXY_PORT.value, socksPort)

        logger.info(
            " **** Setting system proxy values to " +
                    "${ProxyEnvVar.HTTPS_PROXY_HOST.value}:${ProxyEnvVar.HTTPS_PROXY_PORT.value}"
        )
    }

    fun disableSystemProxy() {
        System.clearProperty(ProxyEnvVar.USE_SYSTEM_PROXIES.value)
        System.clearProperty(ProxyEnvVar.HTTP_PROXY_HOST.value)
        System.clearProperty(ProxyEnvVar.HTTP_PROXY_PORT.value)
        System.clearProperty(ProxyEnvVar.HTTPS_PROXY_HOST.value)
        System.clearProperty(ProxyEnvVar.HTTPS_PROXY_PORT.value)
        System.clearProperty(ProxyEnvVar.SOCKS_PROXY_HOST.value)
        System.clearProperty(ProxyEnvVar.SOCKS_PROXY_PORT.value)

        logger.info(" **** Unsetting system proxy values ")
    }

}