package io.horizontalsystems.bankwallet.modules.manageaccount

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingType
import io.horizontalsystems.bankwallet.core.managers.RestoreSettingsManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class ManageAccountService(
        accountId: String,
        private val accountManager: IAccountManager,
        private val walletManager: IWalletManager,
        private val restoreSettingsManager: RestoreSettingsManager
) : Clearable {
    private val disposable = CompositeDisposable()

    private val accountSubject = PublishSubject.create<Account>()
    val accountObservable: Flowable<Account> = accountSubject.toFlowable(BackpressureStrategy.BUFFER)
    var account: Account = accountManager.account(accountId)!!
        private set(value) {
            field = value
            accountSubject.onNext(value)
        }

    private var newName = account.name

    private val stateSubject = PublishSubject.create<State>()
    val stateObservable: Flowable<State> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)
    var state: State = State.CannotSave
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    private val accountDeletedSubject = PublishSubject.create<Unit>()
    val accountDeletedObservable: Flowable<Unit> = accountDeletedSubject.toFlowable(BackpressureStrategy.BUFFER)

    val accountSettingsInfo: List<Triple<PlatformCoin, RestoreSettingType, String>>
        get() {
            val accountWallets = walletManager.getWallets(account)
            return restoreSettingsManager.accountSettingsInfo(account).mapNotNull { (coinType, restoreSettingType, value) ->
                val wallet = accountWallets.find { it.coinType == coinType }
                if (wallet == null || (restoreSettingType == RestoreSettingType.BirthdayHeight && value.isEmpty())) {
                    null
                } else {
                    Triple(wallet.platformCoin, restoreSettingType, value)
                }
            }
        }

    init {
        accountManager.accountsFlowable
                .subscribeIO { handleUpdatedAccounts(it) }
                .let { disposable.add(it) }

        syncState()
    }

    private fun syncState() {
        state = when {
            newName.isNotEmpty() && newName != account.name -> State.CanSave
            else -> State.CannotSave
        }
    }

    private fun handleUpdatedAccounts(accounts: List<Account>) {
        val account = accounts.find { it.id == account.id }
        if (account != null) {
            this.account = account
        } else {
            accountDeletedSubject.onNext(Unit)
        }
    }

    fun setName(name: String) {
        newName = name.trim().replace("\n", " ")
        syncState()
    }

    fun saveAccount() {
        account.name = newName
        accountManager.update(account)
    }

    fun getSettingsTitle(settingType: RestoreSettingType, platformCoin: PlatformCoin): String {
        return restoreSettingsManager.getSettingsTitle(settingType, platformCoin)
    }

    override fun clear() {
        disposable.clear()
    }

    fun deleteAccount() {
        accountManager.delete(account.id)
    }

    enum class State {
        CanSave, CannotSave
    }

}
