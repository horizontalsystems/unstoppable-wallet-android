package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import com.google.gson.GsonBuilder
import com.trustwallet.walletconnect.WCClient
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import okhttp3.OkHttpClient
import java.util.*

class WalletConnectInteractor(private val session: WCSession, private val peerId: String = UUID.randomUUID().toString(), private val remotePeerId: String? = null) {

    interface Delegate {
        fun didRequestSession(peerMeta: WCPeerMeta)
    }

    constructor(uri: String) : this(WCSession.from(uri) ?: throw SessionError.InvalidUri)

    private val clientMeta = WCPeerMeta("Unstoppable Wallet", "https://unstoppable.money")
    var delegate: Delegate? = null

    val client = WCClient(GsonBuilder(), OkHttpClient.Builder().build())

    init {
        client.onSessionRequest = { id: Long, peer: WCPeerMeta ->
            Log.e("AAA", "client.onSessionRequest")
            delegate?.didRequestSession(peer)
        }
    }

    fun connect() {
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

