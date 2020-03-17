package io.horizontalsystems.bankwallet.modules.restore

import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType

class RestoreRouter : RestoreModule.IRouter {

    val showRestoreCoins = SingleLiveEvent<PredefinedAccountType>()
    val showKeyInputEvent = SingleLiveEvent<PredefinedAccountType>()
    val closeEvent = SingleLiveEvent<Unit>()
    val closeWithSuccessEvent = SingleLiveEvent<Unit>()
    val startMainModuleLiveEvent = SingleLiveEvent<Unit>()

    override fun showKeyInput(predefinedAccountType: PredefinedAccountType) {
        showKeyInputEvent.postValue(predefinedAccountType)
    }

    override fun showRestoreCoins(predefinedAccountType: PredefinedAccountType) {
        showRestoreCoins.postValue(predefinedAccountType)
    }

    override fun startMainModule() {
        startMainModuleLiveEvent.call()
    }

    override fun close() {
        closeEvent.call()
    }

    override fun closeWithSuccess() {
        closeWithSuccessEvent.call()
    }
}
