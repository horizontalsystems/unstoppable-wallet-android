package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.core.InvalidContractAddress
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.addtoken.bep2.AddBep2TokenBlockchainService
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class AddTokenService(
        private val coinManager: ICoinManager,
        private val evmService: AddEvmTokenBlockchainService,
        private val bep2Service: AddBep2TokenBlockchainService,
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val erc20NetworkType: EthereumKit.NetworkType,
        private val bep20NetworkType: EthereumKit.NetworkType) {

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

        if (isValidEvmAddress(referenceNonNull)) {
            //check for existing tokens
            coinManager.getCoin(evmService.coinType(referenceNonNull, erc20NetworkType))?.let {
                state = AddTokenModule.State.AlreadyExists(it)
                return
            }
            coinManager.getCoin(evmService.coinType(referenceNonNull, bep20NetworkType))?.let {
                state = AddTokenModule.State.AlreadyExists(it)
                return
            }
            state = AddTokenModule.State.Loading

            //get token info from Erc20 and Bep20 APIs
            disposable = evmService.coinAsync(referenceNonNull, erc20NetworkType)
                    .onErrorResumeNext(evmService.coinAsync(referenceNonNull, bep20NetworkType))
                    .subscribeOn(Schedulers.io())
                    .subscribe({ coin ->
                        state = AddTokenModule.State.Fetched(coin)
                    }, { error ->
                        state = AddTokenModule.State.Failed(error)
                    })

        } else if (isValidBep2Address(referenceNonNull)) {
            //get token info from Bep2
            coinManager.getCoin(bep2Service.coinType(referenceNonNull))?.let {
                state = AddTokenModule.State.AlreadyExists(it)
                return
            }
            state = AddTokenModule.State.Loading
            //fetch token info from network
            disposable = bep2Service.coinAsync(referenceNonNull)
                    .subscribeOn(Schedulers.io())
                    .subscribe({ coin ->
                        state = AddTokenModule.State.Fetched(coin)
                    }, { error ->
                        state = AddTokenModule.State.Failed(error)
                    })
        } else {
            //show invalid address error
            state = AddTokenModule.State.Failed(InvalidContractAddress())
        }

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

    private fun isValidEvmAddress(referenceNonNull: String): Boolean {
        return try {
            evmService.validate(referenceNonNull)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidBep2Address(referenceNonNull: String): Boolean {
        return try {
            bep2Service.validate(referenceNonNull)
            true
        } catch (e: Exception) {
            false
        }
    }
}
