package io.horizontalsystems.bankwallet.modules.derivatoinsettings

import io.horizontalsystems.core.SingleLiveEvent

class DerivationSettingsRouter: DerivationSettingsModule.IRouter {

    val close = SingleLiveEvent<Unit>()

    override fun close() {
        close.call()
    }

}
