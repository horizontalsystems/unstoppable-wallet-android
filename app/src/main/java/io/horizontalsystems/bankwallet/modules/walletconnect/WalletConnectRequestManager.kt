package io.horizontalsystems.bankwallet.modules.walletconnect

class WalletConnectRequestManager {
    private val pendingRequests = hashMapOf<String, MutableList<WalletConnectRequest>>()

    fun getNextRequest(peerId: String): WalletConnectRequest? {
        return pendingRequests[peerId]?.firstOrNull()
    }

    fun save(peerId: String, request: WalletConnectRequest) {
        if (!pendingRequests.containsKey(peerId)) {
            pendingRequests[peerId] = mutableListOf()
        }
        pendingRequests[peerId]?.add(request)
    }

    fun remove(peerId: String, requestId: Long): WalletConnectRequest? {
        val request = pendingRequests[peerId]?.firstOrNull { it.id == requestId }
        request?.let {
            pendingRequests[peerId]?.remove(it)
        }
        return request
    }

}
