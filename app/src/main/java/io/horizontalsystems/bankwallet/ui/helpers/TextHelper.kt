package io.horizontalsystems.bankwallet.ui.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IClipboardManager

object TextHelper : IClipboardManager {

    override val hasPrimaryClip: Boolean
        get() = clipboard?.hasPrimaryClip() ?: false

    override fun copyText(text: String) {
        copyTextToClipboard(App.instance, text)
    }

    override fun getCopiedText(): String {
        return clipboard?.primaryClip?.itemCount?.let { count ->
            if (count > 0) {
                clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
            } else {
                null
            }
        } ?: ""
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
