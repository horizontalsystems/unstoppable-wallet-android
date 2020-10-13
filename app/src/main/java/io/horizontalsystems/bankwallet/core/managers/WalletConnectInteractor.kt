package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import com.trustwallet.walletconnect.models.session.WCSessionUpdate
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.*

class WalletConnectInteractor(val session: WCSession, val peerId: String = UUID.randomUUID().toString()) {

    interface Delegate {
        fun didConnect()
        fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta)
        fun didKillSession()
        fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction)
        fun didRequestSignEthTransaction(id: Long, transaction: WCEthereumTransaction)
    }

    constructor(uri: String) : this(WCSession.from(uri) ?: throw SessionError.InvalidUri)

    private val clientMeta = WCPeerMeta("Unstoppable Wallet", "https://unstoppable.money")
    var delegate: Delegate? = null

    val client = WCClient(GsonBuilder(), OkHttpClient.Builder().build())

    init {
        client.onSessionRequest = { id: Long, peer: WCPeerMeta ->
            client.remotePeerId?.let { delegate?.didRequestSession(it, peer) }
        }

        client.onSessionUpdate = { id: Long, update: WCSessionUpdate ->
            if (!update.approved) {
                delegate?.didKillSession()
            }
        }

        client.addSocketListener(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                delegate?.didConnect()
            }
        })

        client.onEthSendTransaction = { id: Long, transaction: WCEthereumTransaction ->
            delegate?.didRequestSendEthTransaction(id, transaction)
        }

        client.onEthSignTransaction = { id: Long, transaction: WCEthereumTransaction ->
            delegate?.didRequestSignEthTransaction(id, transaction)
        }
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
        delegate?.didKillSession()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun <T> approveRequest(id: Long, result: T) {
        client.approveRequest(id, result)
    }

    fun rejectRequest(id: Long, message: String) {
        client.rejectRequest(id, message)
    }

    sealed class SessionError : Error() {
        object InvalidUri : SessionError()
    }
}

