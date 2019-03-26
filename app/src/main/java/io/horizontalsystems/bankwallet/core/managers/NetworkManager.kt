package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.Flowable
import io.reactivex.Maybe
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class NetworkManager(private val appConfig: IAppConfigProvider) : INetworkManager {

    override fun getRate(coinCode: String, currency: String, timestamp: Long): Maybe<BigDecimal> {
        val apiService = ServiceExchangeApi.service(appConfig.ipfsUrl)

        val hourFlowable = apiService.getRateByHour(coinCode, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd/HH"))
        val dayFlowable = apiService.getRateByDay(coinCode, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd"))

        return hourFlowable
                .flatMap { minuteRates ->
                    Maybe.just(minuteRates.getValue(DateHelper.formatDateInUTC(timestamp, "mm")).toBigDecimal())
                }
                .onErrorResumeNext(dayFlowable.map { it.toBigDecimal() })
    }

    override fun getLatestRateData(currency: String): Flowable<LatestRateData> {
        return ServiceExchangeApi.service(appConfig.ipfsUrl)
                .getLatestRate(currency)
                .onErrorResumeNext(Flowable.empty())
    }

    override fun getTransaction(host: String, path: String): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host)
                .getFullTransaction(path)
    }

    override fun ping(host: String, url: String): Flowable<Any> {
        return ServicePing.service(host)
                .ping(url)
    }
}

object ServiceExchangeApi {

    fun service(apiURL: String): IExchangeRate {
        return APIClient.retrofit(apiURL)
                .create(IExchangeRate::class.java)
    }

    interface IExchangeRate {

        @GET("xrates/historical/{coin}/{fiat}/{datePath}/index.json")
        fun getRateByDay(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Maybe<String>

        @GET("xrates/historical/{coin}/{fiat}/{datePath}/index.json")
        fun getRateByHour(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Maybe<Map<String, String>>

        @GET("xrates/latest/{fiat}/index.json")
        fun getLatestRate(
                @Path("fiat") currency: String
        ): Flowable<LatestRateData>

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
        fun ping(@Url path: String): Flowable<Any>
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
