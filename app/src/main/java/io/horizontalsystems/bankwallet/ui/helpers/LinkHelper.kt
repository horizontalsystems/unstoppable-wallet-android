package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import io.horizontalsystems.bankwallet.R
import java.net.MalformedURLException
import java.net.URL

object LinkHelper {
    fun openLinkInAppBrowser(context: Context, link: String) {
        val urlString = getValidUrl(link) ?: return

        try {
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
        } catch (e: SecurityException) {
            // Fallback to standard intent if Custom Tabs fails
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("LinkHelper", "Failed to open URL: $urlString", e)
            }
        }
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
