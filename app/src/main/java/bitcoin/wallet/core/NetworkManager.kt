package bitcoin.wallet.core

import android.util.Log
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.entities.EuroCurrency
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class NetworkManager : INetworkManager {

    override fun getRate(coinCode: String, currency: String, year: Int, month: String, day: String, hour: String, minute: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getRate(coinCode, currency, year, month, day, hour, minute)
                .onErrorResumeNext(getRateByDay(coinCode, currency, year, month, day))
    }

    override fun getRateByDay(coinCode: String, currency: String, year: Int, month: String, day: String): Flowable<Double> {
        return ServiceExchangeApi.service
                .getRateByDay(coinCode, currency, year, month, day)
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

    override fun getCurrencyCodes(): Flowable<List<Currency>> {
        return ServiceExchangeApi.service .getCurrencies()
                .map { t: String ->  getCurrenciesFromCodes(t) }
                .onErrorReturn {
                    Log.e("NetwMan", "exception: ", it)
                    listOf(DollarCurrency(), EuroCurrency())
                }
    }

    private fun getCurrenciesFromCodes(currencyCodes: String): List<Currency> {
        val codeList = currencyCodes.split(",").map { it.trim() }
        val currencyList = mutableListOf<Currency>()
        codeList.forEach { code ->
            val currency: Currency? = when(code) {
                "USD" -> DollarCurrency()
                "EUR" -> EuroCurrency()
                else -> null
            }
            currency?.let { currencyList.add(it) }
        }
        return currencyList
    }
}

object ServiceExchangeApi {

    val service: IExchangeRate
    private const val apiURL = "http://ipfs.grouvi.org/ipns/QmVefrf2xrWzGzPpERF6fRHeUTh9uVSyfHHh4cWgUBnXpq/io-hs/data/xrates/"

    init {
        val logger = HttpLoggingInterceptor()
        logger.level = HttpLoggingInterceptor.Level.BASIC

        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(logger)

        val retrofit = Retrofit.Builder()
                .baseUrl(apiURL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build()

        service = retrofit.create(IExchangeRate::class.java)
    }

    interface IExchangeRate {

        @GET("{coin}/{fiat}/{year}/{month}/{day}/{hour}/{minute}/index.json")
        fun getRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("year") year: Int,
                @Path("month") month: String,
                @Path("day") day: String,
                @Path("hour") hour: String,
                @Path("minute") minute: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/{year}/{month}/{day}/index.json")
        fun getRateByDay(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String,
                @Path("year") year: Int,
                @Path("month") month: String,
                @Path("day") day: String
        ): Flowable<Double>

        @GET("{coin}/{fiat}/index.json")
        fun getLatestRate(
                @Path("coin") coinCode: String,
                @Path("fiat") currency: String
        ): Flowable<Double>

        @GET("btc/index.json")
        fun getCurrencies(): Flowable<String>

    }
}
