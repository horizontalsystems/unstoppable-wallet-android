package io.horizontalsystems.walletkit.ui.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IClipboardManager

object TextHelper : IClipboardManager {

    // ClipDescription.EXTRA_IS_SENSITIVE was added in API 33, but Gboard and several OEM
    // clipboards honour the same key on earlier releases, so it is set by name for every version.
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    override val hasPrimaryClip: Boolean
        get() = clipboard?.hasPrimaryClip() ?: false

    override fun copyText(text: String) {
        copyTextToClipboard(App.instance, text)
    }

    override fun copySecret(text: String) {
        // An empty label keeps the clip from announcing what it holds, and the sensitive flag asks
        // the system and the keyboard to keep the value out of clipboard previews and history.
        val clip = ClipData.newPlainText("", text).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard?.setPrimaryClip(clip)
    }

    override fun getCopiedText(): String? {
        return clipboard?.primaryClip?.itemCount?.let { count ->
            if (count > 0) {
                clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            } else {
                null
            }
        }
    }

    fun getCleanedUrl(link: String): String{
        var cleanedUrl = link.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(),"")
        if (cleanedUrl.endsWith("/")) {
            cleanedUrl = cleanedUrl.substring(0, cleanedUrl.length - 1)
        }
        return cleanedUrl
    }

    private val clipboard: ClipboardManager?
        get() = App.instance.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    private fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard?.setPrimaryClip(clip)
    }

}
