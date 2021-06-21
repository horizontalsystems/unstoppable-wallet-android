package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.coinkit.models.Coin
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AddTokenService(
        private val coinManager: ICoinManager,
        private val blockchainServices: List<IAddTokenBlockchainService>,
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

        val validServices = blockchainServices.filter { it.isValid(referenceNonNull) }

        if (validServices.isEmpty()) {
            state = AddTokenModule.State.Failed(InvalidContractAddress())
            return
        }

        validServices.forEach { service ->
            val coinType = service.coinType(referenceNonNull)
            coinManager.getCoin(coinType)?.let {
                state = AddTokenModule.State.AlreadyExists(it)
                return
            }
        }

        state = AddTokenModule.State.Loading

        disposable = chainedCoinAsync(validServices, referenceNonNull)
                .subscribeOn(Schedulers.io())
                .subscribe({ coin ->
                    state = AddTokenModule.State.Fetched(coin)
                }, { error ->
                    state = AddTokenModule.State.Failed(error)
                })
    }

    fun save() {
        val coin = (state as? AddTokenModule.State.Fetched)?.coin ?: return
        coinManager.save(listOf(coin))

        val account = accountManager.activeAccount ?: return
        val wallet = Wallet(coin, account)

        walletManager.save(listOf(wallet))
    }

    fun onCleared() {
        disposable?.dispose()
    }

    private fun chainedCoinAsync(services: List<IAddTokenBlockchainService>, reference: String): Single<Coin> {
        val single = services[0].coinAsync(reference)

        return if (services.size == 1) {
            single
        } else {
            val remainedServices = services.drop(1)
            val nextSingle = chainedCoinAsync(remainedServices, reference)
            single.onErrorResumeNext(nextSingle)
        }
    }

}
