package io.horizontalsystems.bankwallet.modules.contact

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.SingleLiveEvent

class ContactView : ContactModule.IView {
    val emailLiveData = MutableLiveData<String>()
    val walletHelpTelegramGroupLiveData = MutableLiveData<String>()
    val developersTelegramGroupLiveData = MutableLiveData<String>()
    val showCopiedLiveEvent = SingleLiveEvent<Unit>()

    override fun setEmail(email: String) {
        emailLiveData.postValue(email)
    }

    override fun setWalletHelpTelegramGroup(group: String) {
        walletHelpTelegramGroupLiveData.postValue(group)
    }

    override fun setDevelopersTelegramGroup(group: String) {
        developersTelegramGroupLiveData.postValue(group)
    }

    override fun showCopied() {
        showCopiedLiveEvent.call()
    }
}
