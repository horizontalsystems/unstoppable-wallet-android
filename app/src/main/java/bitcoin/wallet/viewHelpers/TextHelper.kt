package bitcoin.wallet.viewHelpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

object TextHelper {

    fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("text", text)
        clipboard?.primaryClip = clip
    }

    fun shareExternalText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, text)

        context.startActivity(Intent.createChooser(intent, title))
    }

}
