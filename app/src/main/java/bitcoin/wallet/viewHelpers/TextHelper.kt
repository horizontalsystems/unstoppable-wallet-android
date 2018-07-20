package bitcoin.wallet.viewHelpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IClipboardManager
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder

object TextHelper : IClipboardManager {

    override fun copyText(text: String) {
        copyTextToClipboard(App.instance, text)
    }

    override fun getCopiedText(): String {
        val clipboard = App.instance.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

        return clipboard?.primaryClip?.itemCount?.let { count ->
            if (count > 0) {
                clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            } else {
                null
            }
        } ?: ""

    }

    private fun copyTextToClipboard(context: Context, text: String) {
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

    fun getQrCodeBitmapFromAddress(address: String): Bitmap? {
        val multiFormatWriter = MultiFormatWriter()
        return try {
            val imageSize = LayoutHelper.dp(150F, App.instance)
            val bitMatrix = multiFormatWriter.encode(address, BarcodeFormat.QR_CODE, imageSize, imageSize, hashMapOf(EncodeHintType.MARGIN to 0))
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

}
