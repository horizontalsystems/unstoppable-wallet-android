package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AddTokenService(
        private val coinManager: ICoinManager,
        private val blockchainService: IAddTokenBlockchainService,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager) {

    private val stateSubject = PublishSubject.create<AddTokenModule.State>()

    private var disposable: Disposable? = null

    val stateObservable: Observable<AddTokenModule.State> = stateSubject

    var state: AddTokenModule.State = AddTokenModule.State.Idle
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    fun set(reference: String?) {
        disposable?.dispose()

        val referenceNonNull = if (reference.isNullOrEmpty()) {
            state = AddTokenModule.State.Idle
            return
        } else {
            reference
        }

        try {
            blockchainService.validate(referenceNonNull)
        } catch (e: Exception) {
            state = AddTokenModule.State.Failed(e)
            return
        }

        coinManager.getCoin(blockchainService.coinType(referenceNonNull))?.let {
            state = AddTokenModule.State.AlreadyExists(it)
            return
        }

        state = AddTokenModule.State.Loading

        disposable = blockchainService.coinAsync(referenceNonNull)
                .subscribeOn(Schedulers.io())
                .subscribe({ coin ->
                    state = AddTokenModule.State.Fetched(coin)
                }, { error ->
                    state = AddTokenModule.State.Failed(error)
                })

    }

    fun save() {
        val coin = (state as? AddTokenModule.State.Fetched)?.coin ?: return
        coinManager.save(coin)

        val account = accountManager.account(coin.type) ?: return
        val wallet = Wallet(coin, account)

        walletManager.save(listOf(wallet))
    }

    fun onCleared() {
        disposable?.dispose()
    }
}
