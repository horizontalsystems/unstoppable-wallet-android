package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import io.horizontalsystems.bankwallet.core.ILocalStorage

class BitcoinHodlingInteractor(
        private val storage: ILocalStorage
) : BitcoinHodlingModule.IInteractor {

    override var isLockTimeEnabled: Boolean
        get() = storage.isLockTimeEnabled
        set(enabled) {
            storage.isLockTimeEnabled = enabled
        }

}
