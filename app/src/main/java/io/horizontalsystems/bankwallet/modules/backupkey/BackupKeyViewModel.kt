package io.horizontalsystems.bankwallet.modules.backupkey

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.SingleLiveEvent

class BackupKeyViewModel(
        private val service: BackupKeyService
) : ViewModel() {
    val openUnlockLiveEvent = SingleLiveEvent<Unit>()
    val showKeyLiveEvent = SingleLiveEvent<Unit>()
    val openConfirmationLiveEvent = SingleLiveEvent<Account>()

    val words: List<String>
        get() = service.words

    val passphrase: String
        get() = service.passphrase

    fun onClickShow() {
        if (service.isPinSet) {
            openUnlockLiveEvent.postValue(Unit)
        } else {
            showKeyLiveEvent.postValue(Unit)
        }
    }

    fun onUnlock() {
        showKeyLiveEvent.postValue(Unit)
    }

    fun onClickBackup() {
        openConfirmationLiveEvent.postValue(service.account)
    }

}
