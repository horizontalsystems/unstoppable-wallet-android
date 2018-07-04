package bitcoin.wallet.core

import io.reactivex.Observable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class NetworkManager : INetworkManager {
    override fun getJwtToken(identity: String, pubKeys: Map<Int, String>): Observable<String> {
        return BackendApi.service.getJwtToken(mapOf("identity" to identity))
                .map {
                    it["token"]
                }
    }
}



object BackendApi {

    var service: IGrouviService

    private const val apiURL = "http://192.168.4.15:3000/api/BTC/testnet/"

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
