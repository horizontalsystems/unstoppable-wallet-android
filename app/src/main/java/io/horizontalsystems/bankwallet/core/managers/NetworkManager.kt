package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.GsonBuilder
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.viewHelpers.DateHelper
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
        return ServiceExchangeApi.service
                .getRate(coinCode, currency, DateHelper.formatDateByUsLocale(timestamp, "yyyy/MM/dd/HH/mm"))
                .onErrorResumeNext(getRateByDay(coinCode, currency, DateHelper.formatDateByUsLocale(timestamp, "yyyy/MM/dd")))
    }

    override fun getRateByDay(coinCode: String, currency: String, datePath: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getRateByDay(coinCode, currency, datePath)
                .onErrorReturn {
                    0.0
                }
    }

    override fun getLatestRate(coinCode: String, currency: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getLatestRate(coinCode, currency)
                .onErrorReturn {
                    0.0
                }
    }

}

object ServiceExchangeApi {

    val service: IExchangeRate
    private const val apiURL = "https://ipfs.horizontalsystems.xyz/ipns/QmSxpioQuDSjTH6XiT5q35V7xpJqxmDheEcTRRWyMkMim7/io-hs/data/xrates/"

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

        @GET("{coin}/{fiat}/{year}/{month}/{datePath}/index.json")
        fun getRateByDay(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("datePath") datePath: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/index.json")
        fun getLatestRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String
        ): Flowable<Double>

    }
}
