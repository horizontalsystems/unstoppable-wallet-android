package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Faq
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type
import java.net.URL

object FaqManager {

    private val faqListUrl = App.appConfigProvider.faqUrl

    private val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Faq::class.java, FaqDeserializer(faqListUrl))
            .create()

    fun getFaqList(): Single<List<HashMap<String, Faq>>> {
        return Single.fromCallable {
            val request = Request.Builder()
                    .url(faqListUrl)
                    .build()

            val response = OkHttpClient().newCall(request).execute()

            val listType = object : TypeToken<List<HashMap<String, Faq>>>() {}.type
            val list: List<HashMap<String, Faq>> = gson.fromJson(response.body?.charStream(), listType)
            response.close()

            list
        }
    }

    class FaqDeserializer(faqUrl: String) : JsonDeserializer<Faq> {
        private val faqUrlObj = URL(faqUrl)

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Faq {
            val jsonObject = json.asJsonObject

            return Faq(
                    jsonObject["title"].asString,
                    absolutify(jsonObject["markdown"].asString)
            )
        }

        private fun absolutify(relativeUrl: String?): String {
            return URL(faqUrlObj, relativeUrl).toString()
        }
    }
}
