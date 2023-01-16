package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.modules.balance.HeaderNote
import io.horizontalsystems.bankwallet.modules.balance.faqUrl
import io.horizontalsystems.bankwallet.modules.balance.headerNote
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import io.horizontalsystems.hdwalletkit.HDExtendedKey.DerivedType
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.disposables.CompositeDisposable
import java.net.URL

class ManageAccountViewModel(
    private val service: ManageAccountService,
    private val clearables: List<Clearable>,
    private val faqManager: FaqManager,
    private val languageManager: LanguageManager
) : ViewModel() {
    val disposable = CompositeDisposable()

    var keyActionState by mutableStateOf(KeyActionState.None)
        private set

    val saveEnabledLiveData = MutableLiveData<Boolean>()
    val finishLiveEvent = SingleLiveEvent<Unit>()

    val account: Account
        get() = service.account

    val accountType = account.type
    val showRecoveryPhrase: Boolean by lazy { accountType is AccountType.Mnemonic}
    val showEvmPrivateKey: Boolean by lazy { accountType is AccountType.Mnemonic || accountType is AccountType.EvmPrivateKey }

    val bip32RootKey: HDExtendedKey?
    val accountExtendedPrivateKey: HDExtendedKey?
    val accountExtendedPublicKey: HDExtendedKey?

    val headerNote: HeaderNote
        get() = account.headerNote(false)

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

    fun onChange(name: String?) {
        service.setName(name ?: "")
    }

    fun onSave() {
        service.saveAccount()
        finishLiveEvent.postValue(Unit)
    }

    fun getFaqUrl(headerNote: HeaderNote): String {
        val baseUrl = URL(faqManager.faqListUrl)
        val faqUrl = headerNote.faqUrl(languageManager.currentLocale.language)
        return URL(baseUrl, faqUrl).toString()
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    enum class KeyActionState {
        None, ShowRecoveryPhrase, BackupRecoveryPhrase
    }

}
