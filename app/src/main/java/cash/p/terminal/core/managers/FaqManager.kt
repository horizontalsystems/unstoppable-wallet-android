package cash.p.terminal.core.managers

import android.os.Parcelable
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import cash.p.terminal.BuildConfig
import cash.p.terminal.R
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.entities.Faq
import cash.p.terminal.entities.FaqMap
import cash.p.terminal.modules.markdown.MarkdownFragment
import cash.p.terminal.modules.markdown.localreader.MarkdownLocalFragment
import cash.p.terminal.navigation.slideFromBottom
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.Single
import okhttp3.Request
import timber.log.Timber
import java.lang.reflect.Type
import java.net.URL

object FaqManager {

    private val faqListUrl = AppConfigProvider.faqUrl

    const val faqPathMigrationRequired = "management/migration_required.md"
    const val faqPathMigrationRecommended = "management/migration_recommended.md"
    const val faqPathPrivateKeys = "management/what-are-private-keys-mnemonic-phrase-wallet-seed.md"

    private fun getFaqUrl(faqPath: String, language: String): String =
        URL(URL(faqListUrl), "faq/$language/$faqPath").toString()

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd")
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Faq::class.java, FaqDeserializer(faqListUrl))
        .create()

    fun showFaqPage(path: String, language: String = "en") {
        navigateToMarkdown(
            destinationId = R.id.markdownFragment,
            input = MarkdownFragment.Input(getFaqUrl(path, language), true)
        )
    }

    fun showFaqPage(@StringRes resId: Int) {
        navigateToMarkdown(
            destinationId = R.id.markdownLocalFragment,
            input = MarkdownLocalFragment.Input(resId, true)
        )
    }

    private fun navigateToMarkdown(@IdRes destinationId: Int, input: Parcelable) {
        val nav = rootNavController() ?: run {
            val error = IllegalStateException("FaqManager: root NavController unavailable")
            check(!BuildConfig.DEBUG) { error.message.orEmpty() }
            Timber.e(error)
            return
        }
        nav.slideFromBottom(destinationId, input)
    }

    private fun rootNavController(): NavController? {
        val activity = getKoinInstance<BackgroundManager>().currentActivity ?: return null
        val navHost = activity.supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as? NavHostFragment
        return navHost?.navController
    }

    fun getFaqList(): Single<List<FaqMap>> {
        return Single.fromCallable {
            val request = Request.Builder()
                .url(faqListUrl)
                .build()

            val response = APIClient.okHttpClient.newCall(request).execute()

            val listType = object : TypeToken<List<FaqMap>>() {}.type
            val list: List<FaqMap> = gson.fromJson(response.body?.charStream(), listType)
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
