package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.FullTransactionRecord
import io.reactivex.Flowable
import java.net.URL

class FullTransactionInfoProvider(private val networkManager: INetworkManager, private val adapter: FullTransactionInfoModule.Adapter, private val provider: FullTransactionInfoModule.Provider)
    : FullTransactionInfoModule.FullProvider {

    override val providerName: String get() = provider.name

    override fun url(hash: String): String {
        return provider.url(hash)
    }

    override fun retrieveTransactionInfo(transactionHash: String): Flowable<FullTransactionRecord> {
        val uri = provider.apiUrl(transactionHash)
        val url = URL(uri)

        return networkManager
                .getTransaction("${url.protocol}://${url.host}", uri)
                .map { adapter.convert(it) }
    }
}
