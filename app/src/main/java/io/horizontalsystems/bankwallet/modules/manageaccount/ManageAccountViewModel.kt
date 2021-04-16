package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class ManageAccountViewModel(
        private val service: ManageAccountService,
        private val clearables: List<Clearable>
) : ViewModel() {
    val disposable = CompositeDisposable()

    val keyActionStateLiveData = MutableLiveData<KeyActionState>()
    val saveEnabledLiveData = MutableLiveData<Boolean>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val confirmUnlinkLiveEvent = SingleLiveEvent<Unit>()
    val confirmBackupLiveEvent = SingleLiveEvent<Unit>()

    val accountName: String
        get() = service.account.name

    init {
        service.stateObservable
                .subscribeIO { syncState(it) }
                .let { disposable.add(it) }
        service.accountObservable
                .subscribeIO { syncAccount(it) }
                .let { disposable.add(it) }

        syncState(service.state)
        syncAccount(service.account)
    }

    private fun syncState(state: ManageAccountService.State) {
        when (state) {
            ManageAccountService.State.CanSave -> saveEnabledLiveData.postValue(true)
            ManageAccountService.State.CannotSave -> saveEnabledLiveData.postValue(false)
        }
    }

    private fun syncAccount(account: Account) {
        keyActionStateLiveData.postValue(if (account.isBackedUp) KeyActionState.ShowRecoveryPhrase else KeyActionState.BackupRecoveryPhrase)
    }

    fun onChange(name: String?) {
        service.setName(name ?: "")
    }

    fun onSave() {
        service.saveAccount()
        finishLiveEvent.postValue(Unit)
    }

    fun onUnlink() {
        if (service.account.isBackedUp) {
            confirmUnlinkLiveEvent.postValue(Unit)
        } else {
            confirmBackupLiveEvent.postValue(Unit)
        }
    }

    fun onClickBackup() {
        TODO("not implemented")
    }

    fun onUnlinkConfirm() {
        TODO("not implemented")
    }

    fun onClickActionButton() {
        TODO("not implemented")
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    enum class KeyActionState {
        ShowRecoveryPhrase, BackupRecoveryPhrase
    }

}
