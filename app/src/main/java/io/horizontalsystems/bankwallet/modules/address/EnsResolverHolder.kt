package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.core.App
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object EnsResolverHolder {
    val resolver by lazy {
        val okHttpClient = HttpService.getOkHttpClientBuilder().build()
        val httpService = HttpService(App.appConfigProvider.blocksDecodedEthereumRpc, okHttpClient)
        val web3j = Web3j.build(httpService)

        EnsResolver(web3j)
    }
}
