package cash.p.terminal.trezor.domain

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import cash.p.terminal.trezor.domain.model.TrezorMethod
import cash.p.terminal.trezor.domain.model.TrezorResponse
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import androidx.core.net.toUri

class TrezorDeepLinkManager(
    private val context: Context,
    private val backgroundManager: BackgroundManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var pendingRequestToken: String? = null
    private var pendingResult: CompletableDeferred<TrezorResponse>? = null
    private val mutex = Mutex()

    suspend fun call(
        method: TrezorMethod,
        params: JsonObject = JsonObject(emptyMap()),
    ): TrezorResponse = mutex.withLock {
        val deferred = CompletableDeferred<TrezorResponse>()
        val token = UUID.randomUUID().toString()
        pendingResult = deferred
        pendingRequestToken = token

        val callback = "$CALLBACK_SCHEME://$CALLBACK_HOST?$PARAM_REQUEST_TOKEN=$token"
        val url = buildUrl(method, params, callback)

        // Trezor Suite's MainActivity uses launchMode=singleTask. FLAG_ACTIVITY_NEW_TASK is
        // sufficient to route through onNewIntent when it's already running and to start it
        // cold when it isn't. CLEAR_TASK/CLEAR_TOP would tear down the React Native bridge
        // mid-flight and cause the USB session to detach with Device_Disconnected.
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage(TREZOR_SUITE_PACKAGE)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            pendingRequestToken = null
            pendingResult = null
            throw TrezorSuiteNotInstalledException()
        }

        try {
            coroutineScope {
                val foregroundJob = launch {
                    backgroundManager.stateFlow
                        .dropWhile { it != BackgroundManagerState.EnterBackground }
                        .collect { state ->
                            if (state == BackgroundManagerState.EnterForeground && deferred.isActive) {
                                delay(1.seconds)
                                if (deferred.isActive) {
                                    deferred.completeExceptionally(TrezorCancelledException())
                                }
                            }
                        }
                }
                try {
                    withTimeout(5.minutes) { deferred.await() }
                } finally {
                    foregroundJob.cancel()
                }
            }
        } finally {
            if (pendingResult === deferred) {
                pendingRequestToken = null
                pendingResult = null
            }
        }
    }

    fun onCallbackReceived(requestToken: String, responseJson: String) {
        if (requestToken != pendingRequestToken) return
        val deferred = pendingResult ?: return

        // Invalidate BEFORE completing — prevents replay
        pendingRequestToken = null
        pendingResult = null

        val response = json.decodeFromString<TrezorResponse>(responseJson)
        deferred.complete(response)
    }

    @VisibleForTesting
    internal fun setPendingForTest(token: String, deferred: CompletableDeferred<TrezorResponse>) {
        pendingRequestToken = token
        pendingResult = deferred
    }

    companion object {
        private const val BASE_URL = "https://connect.trezor.io/9/deeplink/1/"
        private const val CALLBACK_SCHEME = "pcash-trezor"
        private const val CALLBACK_HOST = "trezor-result"
        private const val APP_NAME = "P.CASH"
        internal const val PARAM_REQUEST_TOKEN = "requestToken"
        private const val TREZOR_SUITE_PACKAGE = "io.trezor.suite"

        fun buildUrl(
            method: TrezorMethod,
            params: JsonObject,
            callback: String
        ): String {
            val encodedParams = URLEncoder.encode(params.toString(), "UTF-8")
            val encodedCallback = URLEncoder.encode(callback, "UTF-8")
            val encodedAppName = URLEncoder.encode(APP_NAME, "UTF-8")
            return "${BASE_URL}?method=${method.value}" +
                "&params=$encodedParams" +
                "&callback=$encodedCallback" +
                "&appName=$encodedAppName"
        }
    }
}
