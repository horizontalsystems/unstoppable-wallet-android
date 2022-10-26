package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDExtendedKey.DerivedType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.disposables.CompositeDisposable

class ManageAccountViewModel(
    private val service: ManageAccountService,
    private val clearables: List<Clearable>
) : ViewModel() {
    val disposable = CompositeDisposable()

    var keyActionState by mutableStateOf(KeyActionState.None)
        private set

    val saveEnabledLiveData = MutableLiveData<Boolean>()
    val finishLiveEvent = SingleLiveEvent<Unit>()
    val additionalViewItemsLiveData = MutableLiveData<List<AdditionalViewItem>>()

    val account: Account
        get() = service.account

    val accountType = account.type
    val showRecoveryPhrase: Boolean by lazy { accountType is AccountType.Mnemonic}
    val showEvmPrivateKey: Boolean by lazy { accountType is AccountType.Mnemonic || accountType is AccountType.EvmPrivateKey }

    val bip32RootKey: HDExtendedKey?
    val accountExtendedPrivateKey: HDExtendedKey?
    val accountExtendedPublicKey: HDExtendedKey?

    init {
        val hdExtendedKey = (accountType as? AccountType.HdExtendedKey)?.hdExtendedKey

        bip32RootKey = if (accountType is AccountType.Mnemonic) {
            val seed = Mnemonic().toSeed(accountType.words, accountType.passphrase)
            HDExtendedKey(seed, HDWallet.Purpose.BIP44)
        } else if (hdExtendedKey?.derivedType == DerivedType.Master) {
            hdExtendedKey
        } else {
            null
        }

        accountExtendedPrivateKey = if (hdExtendedKey?.derivedType == DerivedType.Account && !hdExtendedKey.info.isPublic) {
            hdExtendedKey
        } else {
            null
        }

        accountExtendedPublicKey = if (hdExtendedKey?.derivedType == DerivedType.Account && hdExtendedKey.info.isPublic) {
            hdExtendedKey
        } else {
            null
        }

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
        keyActionState = when {
            account.isWatchAccount -> KeyActionState.None
            account.isBackedUp -> KeyActionState.ShowRecoveryPhrase
            else -> KeyActionState.BackupRecoveryPhrase
        }
    }

    private fun syncAccountSettings() {
        if (keyActionState == KeyActionState.ShowRecoveryPhrase) {
            val additionalViewItems =
                service.accountSettingsInfo.map { (token, restoreSettingType, value) ->
                    AdditionalViewItem(
                        token,
                        service.getSettingsTitle(restoreSettingType, token),
                        value
                    )
                }
            additionalViewItemsLiveData.postValue(additionalViewItems)
        }
    }

    fun onChange(name: String?) {
        service.setName(name ?: "")
    }

    fun onSave() {
        service.saveAccount()
        finishLiveEvent.postValue(Unit)
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    enum class KeyActionState {
        None, ShowRecoveryPhrase, BackupRecoveryPhrase
    }

    data class AdditionalViewItem(
        val token: Token,
        val title: String,
        val value: String
    )

}
