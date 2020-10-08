package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.*

class WalletConnectInteractor(val session: WCSession, val peerId: String = UUID.randomUUID().toString()) {

    interface Delegate {
        fun didConnect()
        fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta)
    }

    constructor(uri: String) : this(WCSession.from(uri) ?: throw SessionError.InvalidUri)

    private val clientMeta = WCPeerMeta("Unstoppable Wallet", "https://unstoppable.money")
    var delegate: Delegate? = null

    val client = WCClient(GsonBuilder(), OkHttpClient.Builder().build())

    init {
        client.onSessionRequest = { id: Long, peer: WCPeerMeta ->
            client.remotePeerId?.let { delegate?.didRequestSession(it, peer) }
        }

        client.addSocketListener(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                delegate?.didConnect()
            }
        })
    }

    fun connect(remotePeerId: String?) {
        client.connect(session, clientMeta, peerId, remotePeerId)
    }

    fun approveSession(address: String, chainId: Int) {
        client.approveSession(listOf(address), chainId)
    }

    fun rejectSession() {
        client.rejectSession()
    }

    fun killSession() {
        client.killSession()
    }

    sealed class SessionError : Error() {
        object InvalidUri : SessionError()
    }
}

