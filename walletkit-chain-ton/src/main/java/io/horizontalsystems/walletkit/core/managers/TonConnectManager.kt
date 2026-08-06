package io.horizontalsystems.walletkit.core.managers

import android.content.Context
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.models.Network
import io.horizontalsystems.tonkit.models.SignTransaction
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class TonConnectManager(
    context: Context,
    appName: String,
    appVersion: String,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    val kit = TonConnectKit.getInstance(context, appName, appVersion)
    val transactionSigner = TonKit.getTransactionSigner(TonKit.getTonApi(Network.MainNet))

    // Pending requests are cached so a request arriving before the main screen is
    // composed (cold start from a deep link) is still delivered.
    private val _pendingSendRequest = MutableStateFlow<SignTransaction?>(null)
    val pendingSendRequest: StateFlow<SignTransaction?> = _pendingSendRequest.asStateFlow()

    private val _pendingDappRequest = MutableStateFlow<DAppRequestEntityWrapper?>(null)
    val pendingDappRequest: StateFlow<DAppRequestEntityWrapper?> = _pendingDappRequest.asStateFlow()

    fun start() {
        kit.start()
        scope.launch {
            try {
                kit.sendRequestFlow.collect {
                    _pendingSendRequest.value = it
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "TonConnect send-request collection failed")
            }
        }
    }

    fun onSendRequestHandled() {
        _pendingSendRequest.value = null
    }

    fun onDappRequestHandled() {
        _pendingDappRequest.value = null
    }

    suspend fun handle(scannedText: String, closeAppOnResult: Boolean = false) {
        try {
            val dAppRequest = kit.readData(scannedText)
            _pendingDappRequest.value = DAppRequestEntityWrapper(dAppRequest, closeAppOnResult)
        } catch (e: Throwable) {
            Timber.e(e, "Reading TonConnect request failed")
        }
    }
}

data class DAppRequestEntityWrapper(
    val dAppRequest: DAppRequestEntity,
    val closeAppOnResult: Boolean
)
