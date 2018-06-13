package org.grouvi.wallet.blockchain.info

import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.grouvi.wallet.core.App
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object BlockchainInfoClient {

    var service: IBlockchainInfoService

    private const val blockchainInfoApiURL = "https://blockchain.info/"
    private const val blockchainInfoApiTestURL = "https://testnet.blockchain.info/"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)  // <-- this is the important line!

        val retrofit = Retrofit.Builder()
                .baseUrl(if (App.testMode) blockchainInfoApiTestURL else blockchainInfoApiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IBlockchainInfoService::class.java)
    }

    interface IBlockchainInfoService {
        @GET("multiaddr")
        fun multiaddr(@Query("active") addresses: String): Flowable<ResponseBody>

        @GET("unspent")
        fun unspent(@Query("active") addresses: String): Flowable<ResponseBody>
    }

}

