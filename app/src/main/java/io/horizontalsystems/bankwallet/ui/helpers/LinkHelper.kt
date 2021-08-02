package io.horizontalsystems.bankwallet.ui.helpers

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import io.horizontalsystems.bankwallet.R

object LinkHelper {
    fun openLinkInAppBrowser(context: Context, link: String){
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
        intent.launchUrl(context, Uri.parse(link))
    }
}
