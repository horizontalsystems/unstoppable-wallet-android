package io.horizontalsystems.walletkit.ui.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object TextHelper : IClipboardManager {

    // ClipDescription.EXTRA_IS_SENSITIVE was added in API 33, but Gboard and several OEM
    // clipboards honour the same key on earlier releases, so it is set by name for every version.
    private const val EXTRA_IS_SENSITIVE = "android.content.extra.IS_SENSITIVE"

    // When the copied secret is due to be dropped. It rides along in the clip's own metadata
    // rather than living in this process: the clipboard outlives us, so if Android kills the app
    // while it is in the background a restarted process can still tell that a secret is pending
    // and finish the job. It is a wall-clock deadline and carries nothing secret.
    private const val EXTRA_SECRET_EXPIRES_AT = "io.horizontalsystems.walletkit.SECRET_EXPIRES_AT"

    // Android never expires a clip on its own, so a copied recovery phrase would otherwise sit in
    // the clipboard until something overwrote it — readable the whole time by whatever app is in
    // the foreground, the keyboard included. Long enough to paste into a password manager, short
    // enough not to outlive the reason it was copied.
    private const val SECRET_TTL_MS = 60_000L
    private const val CLEAR_RETRY_MS = 5_000L

    // On a cold start the foreground callback runs before the window takes focus, and Android only
    // lets the focused app read the clipboard — so the first look after launch comes back empty
    // even when a secret is sitting there. Look again a few times before concluding there is
    // nothing pending.
    private const val UNREADABLE_RETRY_MS = 1_000L
    private const val UNREADABLE_ATTEMPTS = 5

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var clearJob: Job? = null

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
                putLong(EXTRA_SECRET_EXPIRES_AT, System.currentTimeMillis() + SECRET_TTL_MS)
            }
        }
        clipboard?.setPrimaryClip(clip)
        startClearWorker()
    }

    /**
     * Drops a copied secret from the clipboard once its lifetime has run out.
     *
     * Called when the app returns to the foreground, including on a cold start after the process
     * was killed: since Android 10 only the foreground app and the active keyboard may touch the
     * clipboard, so a deadline that passes while the app is away is settled on the way back in.
     */
    fun clearSecretIfExpired() {
        startClearWorker()
    }

    /**
     * What the clipboard currently holds, from this app's point of view.
     *
     * [Unreadable] is not the same as holding nothing: on a cold start the foreground callback runs
     * before the window takes focus, and Android only lets the focused app read the clipboard, so
     * an early look comes back blank even when a secret is sitting there.
     */
    private sealed interface ClipboardState {
        data object Unreadable : ClipboardState
        data object NotOurs : ClipboardState
        data class Secret(val expiresAt: Long) : ClipboardState
    }

    /**
     * A clip the user copied since replaces the description along with the text, so the absence of
     * the deadline is also what keeps someone else's clip from being wiped.
     */
    private fun clipboardState(): ClipboardState {
        val description = clipboard?.primaryClipDescription ?: return ClipboardState.Unreadable
        val expiresAt = description.extras?.getLong(EXTRA_SECRET_EXPIRES_AT, 0L) ?: 0L

        return if (expiresAt > 0L) ClipboardState.Secret(expiresAt) else ClipboardState.NotOurs
    }

    @Synchronized
    private fun startClearWorker() {
        if (clearJob?.isActive == true) return

        clearJob = scope.launch {
            // The clipboard is the authority on what is pending, so the deadline is re-read every
            // pass. A secret copied while an older timer is still waiting is picked up here rather
            // than needing the timer to be torn down and replaced.
            var unreadableLooks = 0

            // The clipboard is the authority on what is pending, so it is re-read every pass. A
            // secret copied while an older timer is still waiting is picked up here rather than
            // needing the timer to be torn down and replaced.
            while (isActive) {
                val state = try {
                    clipboardState()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e("TextHelper", "Could not read the clipboard", e)
                    delay(CLEAR_RETRY_MS)
                    continue
                }

                when (state) {
                    ClipboardState.NotOurs -> break

                    ClipboardState.Unreadable -> {
                        if (++unreadableLooks >= UNREADABLE_ATTEMPTS) break
                        delay(UNREADABLE_RETRY_MS)
                    }

                    is ClipboardState.Secret -> {
                        unreadableLooks = 0

                        val remaining = state.expiresAt - System.currentTimeMillis()
                        if (remaining > 0) {
                            delay(remaining)
                            continue
                        }

                        try {
                            clipboard?.clearPrimaryClip()
                            break
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Throwable) {
                            // Keep the deadline and try again rather than dropping the timer and
                            // leaving the secret sitting there until the next foreground event.
                            Log.e("TextHelper", "Could not clear the copied secret", e)
                            delay(CLEAR_RETRY_MS)
                        }
                    }
                }
            }
        }
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
