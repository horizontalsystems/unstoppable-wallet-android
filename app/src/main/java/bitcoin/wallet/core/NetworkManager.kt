package bitcoin.wallet.core

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

class NetworkManager : INetworkManager {
    override fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String> {
        return BackendApi.service
                .getJwtToken(mapOf("identity" to identity))
                .map {
                    it["token"]
                }
    }

    override fun getExchangeRates(): Observable<Map<String, Double>> {
        return ServiceExchangeApi.service
                .getRates("USD", "BTC,BCH,ETH")
                .onErrorReturn {
                    mapOf()
                }
    }
}


object BackendApi {

    var service: IGrouviService

    private const val apiURL = "http://bitnode-db.grouvi.org:3000/api/BTC/testnet/"

    init {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logging)  // <-- this is the important line!

        val retrofit = Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IGrouviService::class.java)
    }

    interface IGrouviService {

        @POST("wallet")
        fun getJwtToken(
                @Body params: Map<String, @JvmSuppressWildcards Any>
        ): Observable<HashMap<String, String>>

    }
}

object ServiceExchangeApi {

    val service: IExchangeRate

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)

        val retrofit = Retrofit.Builder()
                .baseUrl("https://min-api.cryptocompare.com")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IExchangeRate::class.java)
    }

    interface IExchangeRate {

        @GET("data/price")
        fun getRates(
                @Query("fsym") baseCurrency: String,
                @Query("tsyms") coinTypes: String
        ): Observable<Map<String, Double>>

    }
}
