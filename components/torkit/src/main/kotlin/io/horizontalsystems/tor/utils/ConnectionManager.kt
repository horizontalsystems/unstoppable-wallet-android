package io.horizontalsystems.tor.utils

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.*
import java.util.concurrent.TimeUnit
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

object ConnectionManager {

    private val logger = Logger.getLogger("ConnectionManager")

    private val READ_TIMEOUT_MILLISECONDS = 60000
    private val CONNECT_TIMEOUT_MILLISECONDS = 60000

    fun getSocketConnection(networkHost: String, networkPort: Int): Socket {
        return Socket(networkHost, networkPort)
    }

    @Throws(IOException::class)
    fun socks4aSocketConnection(networkHost: String, networkPort: Int, useProxy: Boolean, proxyHost: String = "",
            proxyPort: Int = 0): Socket {

        val socket = Socket()
        val socksAddress: SocketAddress = InetSocketAddress(proxyHost, proxyPort)
        socket.soTimeout = READ_TIMEOUT_MILLISECONDS
        socket.connect(socksAddress, CONNECT_TIMEOUT_MILLISECONDS)

        val outputStream = DataOutputStream(socket.getOutputStream())

        outputStream.writeByte(0x04)
        outputStream.writeByte(0x01)
        outputStream.writeShort(networkPort)
        outputStream.writeInt(0x01)
        outputStream.writeByte(0x00)
        outputStream.write(networkHost.toByteArray())
        outputStream.writeByte(0x00)

        val inputStream = DataInputStream(socket.getInputStream())
        val firstByte: Byte = inputStream.readByte()
        val secondByte: Byte = inputStream.readByte()

        if (firstByte != 0x00.toByte() || secondByte != 0x5a.toByte()) {
            socket.close()
            throw IOException(
                    "SOCKS4a connect failed, got " + firstByte + " - " + secondByte +
                            ", but expected 0x00 - 0x5a:, networkHost= " + networkHost + ", networkPort = " + networkPort
                            + ", socksHost=" + proxyHost + ",socksPort=" + proxyPort
            )
        }
        inputStream.readShort()
        inputStream.readInt()

        return socket
    }

    fun httpURLConnection(url: URL, useProxy: Boolean, proxyHost: String = "", proxyPort: Int = 0): HttpURLConnection {

        if (useProxy) {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            return url.openConnection(proxy) as HttpURLConnection
        } else
            return url.openConnection() as HttpURLConnection
    }

    fun setSystemProxy(userSystemProxy: Boolean, host: String, httpPort: String, socksPort: String) {

        System.setProperty(ProxyEnvVar.USE_SYSTEM_PROXIES.value, userSystemProxy.toString());
        System.setProperty(ProxyEnvVar.HTTP_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.HTTP_PROXY_PORT.value, httpPort)
        System.setProperty(ProxyEnvVar.HTTPS_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.HTTPS_PROXY_PORT.value, httpPort)
        System.setProperty(ProxyEnvVar.SOCKS_PROXY_HOST.value, host)
        System.setProperty(ProxyEnvVar.SOCKS_PROXY_PORT.value, socksPort)

        logger.info(" **** Setting system proxy values to " +
                            "${ProxyEnvVar.HTTPS_PROXY_HOST.value}:${ProxyEnvVar.HTTPS_PROXY_PORT.value}")
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

    fun retrofit(apiURL: String, timeout: Long = 60, useProxy: Boolean = false, proxyHost: String = "",
                 proxyPort: Int = 0)
            : Retrofit {

        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)

        if (useProxy) {
            httpClient.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyHost, proxyPort)))
        }

        httpClient.connectTimeout(timeout, TimeUnit.SECONDS)
        httpClient.readTimeout(timeout, TimeUnit.SECONDS)

        val gsonBuilder = GsonBuilder().setLenient()

        return Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .client(httpClient.build())
                .build()
    }
}