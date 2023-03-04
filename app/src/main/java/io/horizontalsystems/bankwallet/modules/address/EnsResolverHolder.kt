package io.horizontalsystems.bankwallet.modules.address

import io.horizontalsystems.bankwallet.core.App
import org.web3j.ens.EnsResolver
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

object EnsResolverHolder {
    val resolver by lazy {
        val projectSecret = App.appConfigProvider.infuraProjectSecret
        val httpCredentials = okhttp3.Credentials.basic("", projectSecret)

        val okHttpClient = HttpService.getOkHttpClientBuilder().addInterceptor { chain ->
            val request = chain.request()
            val authenticationRequest =
                request.newBuilder().header("Authorization", httpCredentials).build()

            chain.proceed(authenticationRequest)
        }.build()

        val projectId = App.appConfigProvider.infuraProjectId
        val httpService = HttpService("https://mainnet.infura.io/v3/$projectId", okHttpClient)
        val web3j = Web3j.build(httpService)

        EnsResolver(web3j)
    }
}
