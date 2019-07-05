package io.horizontalsystems.bankwallet.modules.restore

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.SingleLiveEvent

class RestoreViewModel : ViewModel(), RestoreModule.View, RestoreModule.Router {

    lateinit var delegate: RestoreModule.ViewDelegate

    val closeLiveEvent = SingleLiveEvent<Unit>()
    val goToRestoreWordsLiveEvent = SingleLiveEvent<Unit>()

    fun init() {
        RestoreModule.init(this, this)
    }

    // Router

    override fun navigateToRestoreWords() {
        goToRestoreWordsLiveEvent.call()
    }

    override fun close() {
        closeLiveEvent.call()
    }
}
