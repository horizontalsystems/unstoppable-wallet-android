package io.horizontalsystems.bankwallet.core.managers

import com.google.gson.*
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategoryMultiLang
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type
import java.net.URL
import java.util.*

object GuidesManager {

    private val guidesUrl = App.appConfigProvider.guidesUrl

    private val gson = GsonBuilder()
            .setDateFormat("yyyy-MM-dd")
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .registerTypeAdapter(Guide::class.java, GuideDeserializer(guidesUrl))
            .create()

    fun getGuideCategories(): Single<Array<GuideCategoryMultiLang>> {
        return Single.fromCallable {
            val request = Request.Builder()
                    .url(guidesUrl)
                    .build()

            val response = OkHttpClient().newCall(request).execute()
            val categories = gson.fromJson(response.body?.charStream(), Array<GuideCategoryMultiLang>::class.java)
            response.close()

            categories
        }
    }

    class GuideDeserializer(guidesUrl: String) : JsonDeserializer<Guide> {
        private val guidesUrlObj = URL(guidesUrl)

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Guide {
            val jsonObject = json.asJsonObject

            return Guide(
                    jsonObject.get("title").asString,
                    context.deserialize(jsonObject.get("updated_at"), Date::class.java),
                    jsonObject["image"].asString?.let { absolutify(it) },
                    absolutify(jsonObject["file"].asString)
            )
        }

        private fun absolutify(relativeUrl: String?): String {
            return URL(guidesUrlObj, relativeUrl).toString()
        }
    }
}
