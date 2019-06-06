package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
import io.reactivex.Flowable
import io.reactivex.Single
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

class NetworkManager(val appConfigProvider: IAppConfigProvider) : INetworkManager {

    override fun getRateByDay(hostType: ServiceExchangeApi.HostType, coinCode: String, currency: String, timestamp: Long): Single<BigDecimal> {
        return historicalRateApiClient(hostType)
                .getRateByDay(coinCode, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd"))
                .map { it.toBigDecimal() }
    }

    override fun getRateByHour(hostType: ServiceExchangeApi.HostType, coinCode: String, currency: String, timestamp: Long): Single<BigDecimal> {
        return historicalRateApiClient(hostType)
                .getRateByHour(coinCode, currency, DateHelper.formatDateInUTC(timestamp, "yyyy/MM/dd/HH"))
                .flatMap { minuteRates ->
                    Single.just(minuteRates.getValue(DateHelper.formatDateInUTC(timestamp, "mm")).toBigDecimal())
                }
    }

    override fun getLatestRateData(hostType: ServiceExchangeApi.HostType, currency: String): Single<LatestRateData> {
        return latestRateApiClient(hostType)
                .getLatestRate(currency)
                .map { LatestRateData(it.rates, it.currency, it.timestamp / 1000) }
    }


    override fun getTransaction(host: String, path: String): Flowable<JsonObject> {
        return ServiceFullTransaction.service(host)
                .getFullTransaction(path)
    }

    override fun ping(host: String, url: String): Flowable<Any> {
        return ServicePing.service(host)
                .ping(url)
    }

    private val latestRateMainClient: ServiceExchangeApi.IExchangeRate = APIClient
            .retrofit("https://${appConfigProvider.ipfsMainGateway}/ipns/${appConfigProvider.ipfsId}/", timeout = 10)
            .create(ServiceExchangeApi.IExchangeRate::class.java)

    private val latestRateFallbackClient: ServiceExchangeApi.IExchangeRate = APIClient
            .retrofit("https://${appConfigProvider.ipfsFallbackGateway}/ipns/${appConfigProvider.ipfsId}/")
            .create(ServiceExchangeApi.IExchangeRate::class.java)

    private val historicalRateMainClient: ServiceExchangeApi.IExchangeRate = APIClient
            .retrofit("https://${appConfigProvider.ipfsMainGateway}/ipns/${appConfigProvider.ipfsId}/", timeout = 10)
            .create(ServiceExchangeApi.IExchangeRate::class.java)

    private val historicalRateFallbackClient: ServiceExchangeApi.IExchangeRate = APIClient
            .retrofit("https://${appConfigProvider.ipfsFallbackGateway}/ipns/${appConfigProvider.ipfsId}/")
            .create(ServiceExchangeApi.IExchangeRate::class.java)

    private fun latestRateApiClient(hostType: ServiceExchangeApi.HostType): ServiceExchangeApi.IExchangeRate {
        return when(hostType) {
            ServiceExchangeApi.HostType.MAIN -> latestRateMainClient
            else -> latestRateFallbackClient
        }
    }

    private fun historicalRateApiClient(hostType: ServiceExchangeApi.HostType): ServiceExchangeApi.IExchangeRate {
        return when(hostType) {
            ServiceExchangeApi.HostType.MAIN -> historicalRateMainClient
            else -> historicalRateFallbackClient
        }
    }
}

object ServiceExchangeApi {

    enum class HostType {
        MAIN, FALLBACK
    }

    interface IExchangeRate {

        @GET("xrates/historical/{coin}/{fiat}/{datePath}/index.json")
        fun getRateByDay(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Single<String>

        @GET("xrates/historical/{coin}/{fiat}/{datePath}/index.json")
        fun getRateByHour(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Single<Map<String, String>>

        @GET("xrates/latest/{fiat}/index.json")
        fun getLatestRate(
                @Path("fiat") currency: String
        ): Single<LatestRateData>

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
