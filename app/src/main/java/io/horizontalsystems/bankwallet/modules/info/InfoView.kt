package io.horizontalsystems.bankwallet.modules.info

import io.horizontalsystems.bankwallet.SingleLiveEvent

class InfoView : InfoModule.IView {
    val titleLiveEvent = SingleLiveEvent<String>()
    val descriptionLiveEvent = SingleLiveEvent<String>()
    val txHashLiveEvent = SingleLiveEvent<String>()
    val conflictingTxHashLiveEvent = SingleLiveEvent<String>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    override fun set(title: String, description: String) {
        titleLiveEvent.postValue(title)
        descriptionLiveEvent.postValue(description)
    }

    override fun setTxHash(txHash: String) {
        txHashLiveEvent.postValue(txHash)
    }

    override fun setConflictingTxHash(conflictingTxHash: String) {
        conflictingTxHashLiveEvent.postValue(conflictingTxHash)
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }

}
