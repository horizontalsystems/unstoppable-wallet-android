package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.CompositeDisposable

class ManageAccountViewModel(
        private val service: ManageAccountService,
        private val clearables: List<Clearable>
) : ViewModel() {
    val disposable = CompositeDisposable()

    val keyActionStateLiveData = MutableLiveData<KeyActionState>()
    val saveEnabledLiveData = MutableLiveData<Boolean>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val additionalViewItemsLiveData = MutableLiveData<List<AdditionalViewItem>>()

    val account: Account
        get() = service.account

    init {
        service.stateObservable
                .subscribeIO { syncState(it) }
                .let { disposable.add(it) }
        service.accountObservable
                .subscribeIO { syncAccount(it) }
                .let { disposable.add(it) }
        service.accountDeletedObservable
                .subscribeIO { finishLiveEvent.postValue(Unit) }
                .let { disposable.add(it) }

        syncState(service.state)
        syncAccount(service.account)
        syncAccountSettings()
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

    private fun syncAccountSettings() {
        val additionalViewItems = service.accountSettingsInfo.map { (platformCoin, restoreSettingType, value) ->
            AdditionalViewItem(platformCoin, service.getSettingsTitle(restoreSettingType, platformCoin), value)
        }
        additionalViewItemsLiveData.postValue(additionalViewItems)
    }

    fun onChange(name: String?) {
        service.setName(name ?: "")
    }

    fun onSave() {
        service.saveAccount()
        finishLiveEvent.postValue(Unit)
    }

    fun onUnlink() {
        service.deleteAccount()
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    enum class KeyActionState {
        ShowRecoveryPhrase, BackupRecoveryPhrase
    }

    data class AdditionalViewItem(
        val platformCoin: PlatformCoin,
        val title: String,
        val value: String
    )

}
