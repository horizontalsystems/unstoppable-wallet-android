package io.horizontalsystems.walletkit.ui.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.os.SystemClock
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

object TextHelper : IClipboardManager {

    // ClipDescription.EXTRA_IS_SENSITIVE was added in API 33, but Gboard and several OEM
    // clipboards honour the same key on earlier releases, so it is set by name for every version.
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    // Android never expires a clip on its own, so a copied recovery phrase would otherwise sit in
    // the clipboard until something overwrote it — readable the whole time by whatever app is in
    // the foreground, the keyboard included. Long enough to paste into a password manager, short
    // enough not to outlive the reason it was copied.
    private const val SECRET_TTL_MS = 60_000L

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var clearJob: Job? = null

    // The digest, not the text: this outlives the screen that copied it, and there is no reason to
    // keep a second plaintext copy of a seed phrase alive in a long-lived static.
    private var copiedSecretDigest: ByteArray? = null
    private var clearAt = 0L

    override val hasPrimaryClip: Boolean
        get() = clipboard?.hasPrimaryClip() ?: false

    override fun copyText(text: String) {
        copyTextToClipboard(App.instance, text)
    }

    override fun copySecret(text: String) {
        // An empty label keeps the clip from announcing what it holds, and the sensitive flag asks
        // supported system and keyboard clients to redact the value instead of showing it in
        // previews and clipboard history. Neither is a boundary: the clip itself stays readable.
        val clip = ClipData.newPlainText("", text).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard?.setPrimaryClip(clip)

        copiedSecretDigest = digestOf(text)
        clearAt = SystemClock.elapsedRealtime() + SECRET_TTL_MS
        scheduleClear(SECRET_TTL_MS)
    }

    /**
     * Drops a previously copied secret from the clipboard once its lifetime has run out.
     *
     * Also called when the app returns to the foreground: since Android 10 only the foreground app
     * and the active keyboard may touch the clipboard, so a timer that fires while the app is in
     * the background cannot do the clearing itself and leaves it to the next resume.
     */
    fun clearSecretIfExpired() {
        val digest = copiedSecretDigest ?: return

        val remaining = clearAt - SystemClock.elapsedRealtime()
        if (remaining > 0) {
            scheduleClear(remaining)
            return
        }

        when (val current = getCopiedText()) {
            // Unreadable rather than absent — the clipboard is off limits from here. Keep the
            // pending state so the next resume can finish the job.
            null -> return

            // The user has copied something else since; that clip is theirs, not ours to wipe.
            else -> if (digestOf(current).contentEquals(digest)) {
                clipboard?.clearPrimaryClip()
            }
        }

        forgetSecret()
    }

    private fun scheduleClear(delayMs: Long) {
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(delayMs)
            clearSecretIfExpired()
        }
    }

    private fun forgetSecret() {
        clearJob?.cancel()
        clearJob = null
        copiedSecretDigest = null
        clearAt = 0L
    }

    private fun digestOf(text: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray())

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
