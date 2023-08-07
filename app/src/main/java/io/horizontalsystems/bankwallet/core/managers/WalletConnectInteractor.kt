package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.ethereum.WCEthereumSignMessage
import com.trustwallet.walletconnect.models.ethereum.WCEthereumTransaction
import com.trustwallet.walletconnect.models.session.WCSession
import com.trustwallet.walletconnect.models.session.WCSessionUpdate
import io.horizontalsystems.bankwallet.core.App
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID

class WalletConnectInteractor(
        val session: WCSession,
        val peerId: String = UUID.randomUUID().toString(),
        private val remotePeerId: String? = null
) {

    interface Delegate {
        fun didUpdateState(state: State) {}
        fun didRequestSession(remotePeerId: String, remotePeerMeta: WCPeerMeta, chainId: Int?) {}
        fun didKillSession() {}
        fun didRequestSendEthTransaction(id: Long, transaction: WCEthereumTransaction) {}
        fun didRequestSignMessage(id: Long, message: WCEthereumSignMessage) {}
        fun didReceiveError(error: Throwable) {}
    }

    sealed class State {
        object Idle: State()
        object Connecting : State()
        object Connected : State()
        class Disconnected(val error: Throwable = Error("Disconnected")) : State()
    }

    var state: State = State.Idle
        private set(value) {
            field = value

            delegate?.didUpdateState(value)
        }

    constructor(uri: String) : this(WCSession.from(uri) ?: throw SessionError.InvalidUri)

    private val clientMeta = WCPeerMeta(
        App.appConfigProvider.walletConnectV1PeerMetaName,
        App.appConfigProvider.walletConnectV1PeerMetaUrl
    )
    var delegate: Delegate? = null

    private val client = WCClient(GsonBuilder(), OkHttpClient.Builder().build())

    init {
        client.addSocketListener(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                state = State.Connected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                state = State.Disconnected(SessionError.SocketDisconnected(t))
            }
        })

        client.onSessionRequest = { _: Long, peerMeta: WCPeerMeta ->
            client.remotePeerId?.let { delegate?.didRequestSession(it, peerMeta, client.chainId?.toIntOrNull()) }
        }

        client.onSessionUpdate = { _: Long, update: WCSessionUpdate ->
            if (!update.approved) {
                delegate?.didKillSession()
            }
        }

        client.onFailure = {
            delegate?.didReceiveError(it)
        }

        client.onDisconnect = { _: Int, _: String ->
            state = State.Disconnected()
        }

        client.onEthSendTransaction = { id: Long, transaction: WCEthereumTransaction ->
            delegate?.didRequestSendEthTransaction(id, transaction)
        }

        client.onEthSign = { id, message ->
            delegate?.didRequestSignMessage(id, message)
        }

        client.onEthSignTransaction = { id, _ -> rejectWithNotSupported(id) }
        client.onCustomRequest = { id, _ -> rejectWithNotSupported(id) }
        client.onBnbTrade = { id, _ -> rejectWithNotSupported(id) }
        client.onBnbCancel = { id, _ -> rejectWithNotSupported(id) }
        client.onBnbTransfer = { id, _ -> rejectWithNotSupported(id) }
        client.onBnbTxConfirm = { id, _ -> rejectWithNotSupported(id) }
        client.onGetAccounts = { id -> rejectWithNotSupported(id) }
        client.onSignTransaction = { id, _ -> rejectWithNotSupported(id) }
    }

    fun connect() {
        state = State.Connecting

        client.connect(session, clientMeta, peerId, remotePeerId ?: client.remotePeerId)
    }

    fun approveSession(address: String, chainId: Int) {
        client.approveSession(listOf(address), chainId)
    }

    fun rejectSession(message: String) {
        client.rejectSession(message)
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

    private fun rejectWithNotSupported(id: Long) {
        client.rejectRequest(id, "Not supported")
    }

    sealed class SessionError : Error() {
        object InvalidUri : SessionError()
        class SocketDisconnected(override val cause: Throwable) : SessionError() {
            override val message = "Socket connection could not be established. ${cause.message}"
        }
    }
}

