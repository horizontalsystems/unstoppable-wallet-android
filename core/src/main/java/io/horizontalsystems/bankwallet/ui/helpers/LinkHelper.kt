package io.horizontalsystems.bankwallet.ui.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.App
import java.net.MalformedURLException
import java.net.URL

object LinkHelper {
    // Opens the Telegram app directly via tg://resolve; falls back to the web link if it isn't installed.
    // Accepts values like "https://t.me/Support", "t.me/Support", "@Support" or "Support".
    fun openTelegram(context: Context, telegramValue: String) {
        val domain = telegramDomain(telegramValue)
        if (domain.isBlank()) {
            openLinkInAppBrowser(context, telegramValue)
            return
        }

        // Build URIs via Uri.Builder so the domain is properly percent-encoded — this prevents
        // malformed links or query-param injection if it contains characters like &, #, or spaces.
        val appUri = Uri.Builder()
            .scheme("tg")
            .authority("resolve")
            .appendQueryParameter("domain", domain)
            .build()

        try {
            val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val webUri = Uri.Builder()
                .scheme("https")
                .authority("t.me")
                .appendPath(domain)
                .build()
            openLinkInAppBrowser(context, webUri.toString())
        }
    }

    private fun telegramDomain(value: String): String {
        return value.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("t.me/")
            .removePrefix("telegram.me/")
            .removePrefix("@")
            .substringBefore("/")
            .substringBefore("?")
    }

    fun openLinkInAppBrowser(context: Context, link: String) {
        val urlString = getValidUrl(link) ?: return

        try {
            openInInternalBrowser(context, urlString)
        } catch (e: Exception) {
            // Fallback to standard intent if Custom Tabs fails
            try {
                openInExternalBrowser(urlString, context)
            } catch (e: Exception) {
                Toast.makeText(App.instance, context.getString(R.string.Error_BrowserNotFound), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openInExternalBrowser(urlString: String, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openInInternalBrowser(context: Context, urlString: String) {
        val builder = CustomTabsIntent.Builder()

        val color = context.getColor(R.color.tyler)

        val params = CustomTabColorSchemeParams.Builder()
            .setNavigationBarColor(color)
            .setToolbarColor(color)
            .build()

        builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, params)
        builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, params)
        builder.setStartAnimations(context, R.anim.slide_from_right, R.anim.slide_to_left)
        builder.setExitAnimations(
            context,
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right
        )

        val intent = builder.build()
        intent.launchUrl(context, Uri.parse(urlString))
    }

    private fun getValidUrl(urlString: String): String? {
        if (urlString.isBlank())
            return null

        val url = createUrl(urlString) ?: createUrl(urlString, "https://") ?: return null

        return url.toString()
    }

    private fun createUrl(urlString: String, protocol: String = ""): URL? {
        return try {
            URL("$protocol$urlString")
        } catch (e: MalformedURLException) {
            null
        }
    }
}
