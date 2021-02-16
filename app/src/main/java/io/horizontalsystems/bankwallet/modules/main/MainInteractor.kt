package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IBackupManager
import io.horizontalsystems.bankwallet.core.IRateAppManager
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.disposables.CompositeDisposable

class MainInteractor(
        private val rateAppManager: IRateAppManager,
        private val backupManager: IBackupManager,
        private val termsManager: ITermsManager,
        private val pinComponent: IPinComponent,
        private val accountManager: IAccountManager)
    : MainModule.IInteractor {

    var delegate: MainModule.IInteractorDelegate? = null
    val disposables = CompositeDisposable()

    override fun onStart() {
        rateAppManager.showRateAppObservable
                .subscribe {
                    delegate?.showRateApp(it)
                }
                .let {
                    disposables.add(it)
                }

        disposables.add(backupManager.allBackedUpFlowable.subscribe {
            delegate?.updateBadgeVisibility()
        })

        disposables.add(termsManager.termsAcceptedSignal.subscribe {
            delegate?.updateBadgeVisibility()
        })

        disposables.add(pinComponent.pinSetFlowable.subscribe {
            delegate?.updateBadgeVisibility()
        })

        disposables.add(accountManager.accountsFlowable.subscribe {
            delegate?.sync(it)
        })

        delegate?.sync(accountManager.accounts)
    }

    override val allBackedUp: Boolean
        get() = backupManager.allBackedUp

    override val termsAccepted: Boolean
        get() = termsManager.termsAccepted

    override val isPinSet: Boolean
        get() = pinComponent.isPinSet

    override fun clear() {
        disposables.clear()
    }
}
