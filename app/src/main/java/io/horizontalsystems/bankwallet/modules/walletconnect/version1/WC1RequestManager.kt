package io.horizontalsystems.bankwallet.modules.walletconnect.version1

class WC1RequestManager {
    private val pendingRequests = hashMapOf<String, MutableList<WC1Request>>()

    fun getNextRequest(peerId: String): WC1Request? {
        return pendingRequests[peerId]?.firstOrNull()
    }

    fun save(peerId: String, request: WC1Request) {
        if (!pendingRequests.containsKey(peerId)) {
            pendingRequests[peerId] = mutableListOf()
        }
        pendingRequests[peerId]?.add(request)
    }

    fun remove(peerId: String, requestId: Long): WC1Request? {
        val request = pendingRequests[peerId]?.firstOrNull { it.id == requestId }
        request?.let {
            pendingRequests[peerId]?.remove(it)
        }
        return request
    }

}
