package io.horizontalsystems.bankwallet.modules.torpage

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetManager

class TorPageInteractor(
        private var netManager: INetManager,
        private val localStorage: ILocalStorage
        ) : TorPageModule.IInteractor {

    override var isTorEnabled: Boolean
        get() = localStorage.torEnabled
        set(value) {
            localStorage.torEnabled = value
        }

    override fun enableTor() {
        netManager.start()
    }

    override fun disableTor() {
        netManager.stop()
    }
}
