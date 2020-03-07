package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.reactivex.Flowable
import java.net.URL

class FullTransactionInfoProvider(private val networkManager: INetworkManager,
                                  private val adapter: FullTransactionInfoModule.Adapter,
                                  private val provider: FullTransactionInfoModule.Provider)
    : FullTransactionInfoModule.FullProvider {

    override val providerName: String get() = provider.name

    override fun url(hash: String): String? {
        return provider.url(hash)
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        val request = provider.apiRequest(transactionHash)
        val uri = request.url
        val url = URL(uri)
        val host = "${url.protocol}://${url.host}"

        val requestFlowable = when (request) {
            is FullTransactionInfoModule.Request.GetRequest -> {
                networkManager.getTransaction(host, uri, request.isSafeCall)
            }
            is FullTransactionInfoModule.Request.PostRequest -> {
                networkManager.getTransactionWithPost(host, uri, request.body)
            }
        }

        return requestFlowable.map { adapter.convert(it) }
    }

}
