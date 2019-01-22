package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

class NetworkManager : INetworkManager {

    override fun getRate(coinCode: String, currency: String, timestamp: Long): Flowable<Double> {
        val cleanedCoin = TextHelper.getCleanCoinCode(coinCode)

        return ServiceExchangeApi.service
                .getRatesByHour(cleanedCoin, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd/HH"))
                .flatMap { minuteRates ->
                    val minute = DateHelper.formatDateInUTC(timestamp, "mm")
                    Flowable.just(minuteRates[minute]!!)
                }
                .onErrorResumeNext(
                        ServiceExchangeApi.service
                                .getRate(cleanedCoin, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd"))
                                .onErrorResumeNext(Flowable.empty())
                )
    }

    override fun getLatestRate(coin: String, currency: String): Flowable<LatestRate> {
        val cleanedCoin = TextHelper.getCleanCoinCode(coin)
        return ServiceExchangeApi.service
                .getLatestRate(cleanedCoin, currency)
                .onErrorResumeNext(Flowable.empty())
    }

    override fun getTransaction(host: String, path: String): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host)
                .getFullTransaction(path)
    }

    override fun ping(host: String, url: String): Flowable<JsonObject> {
        return ServicePing.service(host)
                .ping(url)
    }
}

object ServiceExchangeApi {

    val service: IExchangeRate = APIClient
            .retrofit("https://ipfs.horizontalsystems.xyz/ipns/Qmd4Gv2YVPqs6dmSy1XEq7pQRSgLihqYKL2JjK7DMUFPVz/io-hs/data/xrates/")
            .create(IExchangeRate::class.java)

    interface IExchangeRate {

        @GET("{coin}/{fiat}/{datePath}/index.json")
        fun getRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/{datePath}/index.json")
        fun getRatesByHour(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Flowable<Map<String, Double>>

        @GET("{coin}/{fiat}/index.json")
        fun getLatestRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String
        ): Flowable<LatestRate>

    }
}

object ServiceFullTransaction {
    fun service(apiURL: String): FullTransactionAPI {
        return APIClient.retrofit(apiURL)
                .create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        @Headers("Content-Type: application/json")
        fun getFullTransaction(@Url path: String): Flowable<JsonObject>
    }

}

object ServicePing {
    fun service(apiURL: String): FullTransactionAPI {
        return APIClient.retrofit(apiURL, timeout = 8)
                .create(FullTransactionAPI::class.java)
    }

    interface FullTransactionAPI {
        @GET
        fun ping(@Url path: String): Flowable<JsonObject>
    }
}

object APIClient {
    fun retrofit(apiURL: String, timeout: Long = 60): Retrofit {

        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)
        httpClient.connectTimeout(timeout, TimeUnit.SECONDS)
        httpClient.readTimeout(timeout, TimeUnit.SECONDS)

        val gsonBuilder = GsonBuilder().setLenient()

        return Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gsonBuilder.create()))
                .client(httpClient.build())
                .build()
    }
}
