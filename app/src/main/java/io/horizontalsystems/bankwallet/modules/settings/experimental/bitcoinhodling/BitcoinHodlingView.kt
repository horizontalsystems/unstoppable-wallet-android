package io.horizontalsystems.bankwallet.modules.settings.experimental.bitcoinhodling

import io.horizontalsystems.bankwallet.SingleLiveEvent

class BitcoinHodlingView : BitcoinHodlingModule.IView {
    val lockTimeEnabledLiveEvent = SingleLiveEvent<Boolean>()

    override fun setLockTime(enabled: Boolean) {
        lockTimeEnabledLiveEvent.postValue(enabled)
    }

}
