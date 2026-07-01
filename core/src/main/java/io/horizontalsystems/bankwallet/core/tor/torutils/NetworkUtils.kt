package io.horizontalsystems.bankwallet.core.tor.torutils

import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket

class NetworkUtils {

    companion object {

        fun isPortOpen(ip: String?, port: Int, timeout: Int): Boolean {
            return try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeout)
                socket.close()
                true
            } catch (ce: ConnectException) { //ce.printStackTrace();
                false
            } catch (ex: Exception) { //ex.printStackTrace();
                false
            }
        }

    }
}