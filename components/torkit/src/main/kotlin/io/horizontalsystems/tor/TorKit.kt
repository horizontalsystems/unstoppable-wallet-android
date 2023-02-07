package io.horizontalsystems.tor

import android.content.Context
import android.util.Log
import io.horizontalsystems.tor.core.TorConstants
import io.horizontalsystems.tor.utils.ConnectionManager
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import retrofit2.Retrofit
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

class TorKit(context: Context) : TorManager.Listener {

    val torInfoSubject: PublishSubject<Tor.Info> = PublishSubject.create()
    private val torManager = TorManager(context, this)
    private var torStarted = false


    fun startTor(useBridges: Boolean) {
        torStarted = true
        enableProxy()
        torManager.start(useBridges)
    }

    fun stopTor(): Single<Boolean> {
        disableProxy()
        torStarted = false
        return torManager.stop()
    }

    fun getSocketConnection(host: String, port: Int): Socket {
        return ConnectionManager.socks4aSocketConnection(
            host,
            port,
            false,//torManager.getTorInfo().isStarted,
            TorConstants.IP_LOCALHOST,
            TorConstants.SOCKS_PROXY_PORT_DEFAULT.toInt()
        )

    }

    fun getHttpConnection(url: URL): HttpURLConnection {

        return ConnectionManager.httpURLConnection(
            url,
            false,
            TorConstants.IP_LOCALHOST,
            TorConstants.HTTP_PROXY_PORT_DEFAULT.toInt()
        )
    }

    fun buildRetrofit(url: String, timeout: Long = 60): Retrofit {
        return ConnectionManager.retrofit(
            url,
            timeout,
            false,//torManager.getTorInfo().isStarted,
            TorConstants.IP_LOCALHOST,
            TorConstants.SOCKS_PROXY_PORT_DEFAULT.toInt()
        )
    }

    fun enableProxy() {
        ConnectionManager.setSystemProxy(
            true,
            TorConstants.IP_LOCALHOST,
            TorConstants.HTTP_PROXY_PORT_DEFAULT,
            TorConstants.SOCKS_PROXY_PORT_DEFAULT
        )
    }

    fun disableProxy() {
        ConnectionManager.disableSystemProxy()
    }

    override fun statusUpdate(torInfo: Tor.Info) {
        torInfoSubject.onNext(torInfo)
        Log.v(
            "TORKIT",
            "statusUpdate connection: ${torInfo.connection.status} status: ${torInfo.status}"
        )
    }

}
