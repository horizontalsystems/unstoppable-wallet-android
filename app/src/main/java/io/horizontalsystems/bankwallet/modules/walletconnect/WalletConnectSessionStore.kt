package io.horizontalsystems.bankwallet.modules.walletconnect

import com.trustwallet.walletconnect.WCSessionStoreItem
import com.trustwallet.walletconnect.WCSessionStoreType
import com.trustwallet.walletconnect.models.WCPeerMeta
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IPredefinedAccountTypeManager
import io.horizontalsystems.bankwallet.entities.PredefinedAccountType
import io.reactivex.BackpressureStrategy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class WalletConnectSessionStore(
        private val accountManager: IAccountManager,
        private val predefinedAccountTypeManager: IPredefinedAccountTypeManager
) {
    private val wcSessionStoreType = WCSessionStoreType(App.preferences)

    var storedItem: WCSessionStoreItem?
        get() = wcSessionStoreType.session
        set(value) {
            wcSessionStoreType.session = value

            storePeerMetaSubject.onNext(Unit)
        }

    val storedPeerMeta: WCPeerMeta?
        get() = storedItem?.remotePeerMeta

    private val storePeerMetaSubject = PublishSubject.create<Unit>()
    val storePeerMetaSignal = storePeerMetaSubject.toFlowable(BackpressureStrategy.LATEST)

    private val disposable = CompositeDisposable()

    init {
        accountManager.accountsDeletedFlowable
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    if (storedItem != null && !accountStandardIsPresent()) {
                        storedItem = null
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun accountStandardIsPresent(): Boolean {
        return accountManager.accounts.any {
            predefinedAccountTypeManager.predefinedAccountType(it.type) == PredefinedAccountType.Standard
        }
    }

}
