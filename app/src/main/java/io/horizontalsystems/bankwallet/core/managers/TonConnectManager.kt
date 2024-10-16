package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.models.Network
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit

class TonConnectManager(context: Context, val adapterFactory: AdapterFactory) {
    val kit = TonConnectKit.getInstance(context)
    val transactionSigner = TonKit.getTransactionSigner(TonKit.getTonApi(Network.MainNet))

    fun start() {
        kit.start()
    }
}
