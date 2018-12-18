package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
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
import retrofit2.http.Path
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

}

object ServiceExchangeApi {

    val service: IExchangeRate
    private const val apiURL = "https://ipfs.horizontalsystems.xyz/ipns/Qmd4Gv2YVPqs6dmSy1XEq7pQRSgLihqYKL2JjK7DMUFPVz/io-hs/data/xrates/"

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)
        httpClient.connectTimeout(60, TimeUnit.SECONDS)
        httpClient.readTimeout(60, TimeUnit.SECONDS)

        val gsonBuilder = GsonBuilder()
        gsonBuilder.setLenient()
        val gson = gsonBuilder.create()

        val retrofit = Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient.build())
                .build()

        service = retrofit.create(IExchangeRate::class.java)
    }

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
