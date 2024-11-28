package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.models.Network
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TonConnectManager(
    context: Context,
    val adapterFactory: AdapterFactory,
    appName: String,
    appVersion: String,
) {
    val kit = TonConnectKit.getInstance(context, appName, appVersion)
    val transactionSigner = TonKit.getTransactionSigner(TonKit.getTonApi(Network.MainNet))

    val sendRequestFlow by kit::sendRequestFlow
    private val _dappRequestFlow = MutableSharedFlow<DAppRequestEntity>()
    val dappRequestFlow
        get() = _dappRequestFlow.asSharedFlow()

    fun start() {
        kit.start()
    }

    suspend fun handle(scannedText: String) {
        try {
            val dAppRequest = kit.readData(scannedText)
            _dappRequestFlow.emit(dAppRequest)
        } catch (e: Throwable) {

        }
    }
}
