package io.horizontalsystems.bankwallet.modules.info

import io.horizontalsystems.bankwallet.SingleLiveEvent

class InfoView : InfoModule.IView {
    val titleLiveEvent = SingleLiveEvent<String>()
    val descriptionLiveEvent = SingleLiveEvent<String>()

    override fun set(title: String, description: String) {
        titleLiveEvent.postValue(title)
        descriptionLiveEvent.postValue(description)
    }

}
