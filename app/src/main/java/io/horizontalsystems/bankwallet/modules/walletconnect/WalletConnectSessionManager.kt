package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.models.WCPeerMeta
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.core.storage.WalletConnectSessionStorage
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.horizontalsystems.bankwallet.entities.WalletConnectSession
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletConnectSessionManager(
        private val storage: WalletConnectSessionStorage,
        private val accountManager: IAccountManager,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager
) {
    val storedSession: WalletConnectSession?
        get() = storage.getSessions().firstOrNull()

    val storedPeerMeta: WCPeerMeta?
        get() = storedSession?.remotePeerMeta

    private val storePeerMetaSubject = PublishSubject.create<Unit>()
    val storePeerMetaSignal: Flowable<Unit> = storePeerMetaSubject.toFlowable(BackpressureStrategy.LATEST)

    private val disposable = CompositeDisposable()

    init {
        accountManager.accountsDeletedFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (storedSession != null && !accountStandardIsPresent()) {
                        clear()
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    fun store(session: WalletConnectSession) {
        storage.save(session)

        storePeerMetaSubject.onNext(Unit)
    }

    fun clear() {
        storage.deleteAll()

        storePeerMetaSubject.onNext(Unit)
    }

    private fun accountStandardIsPresent(): Boolean {
        return accountManager.accounts.any {
            predefinedAccountTypeManager.predefinedAccountType(it.type) == PredefinedAccountType.Standard
        }
    }

}
