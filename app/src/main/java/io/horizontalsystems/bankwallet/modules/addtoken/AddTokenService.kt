package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.CustomCoin
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Platform
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.*

class AddTokenService(
    private val coinManager: ICoinManager,
    private val blockchainServices: List<IAddTokenBlockchainService>,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager
) {
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
            state = AddTokenModule.State.Failed(TokenError.InvalidReference)
            return
        }
        val existingPlatformCoins = mutableListOf<PlatformCoin>()

        validServices.forEach { service ->
            val coinType = service.coinType(referenceNonNull)
            coinManager.getPlatformCoin(coinType)?.let {
                existingPlatformCoins.add(it)
            }
        }

        if (existingPlatformCoins.isNotEmpty()) {
            state = AddTokenModule.State.AlreadyExists(existingPlatformCoins)
            return
        }

        state = AddTokenModule.State.Loading

        disposable = joinedCustomTokensSingle(validServices, reference)
            .subscribeOn(Schedulers.io())
            .subscribe({ customCoins ->
                state = if (customCoins.isEmpty()) {
                    AddTokenModule.State.Failed(TokenError.NotFound)
                } else {
                    AddTokenModule.State.Fetched(customCoins)
                }
            }, { error ->
                state = AddTokenModule.State.Failed(error)
            })
    }

    fun save() {
        val customCoins = (state as? AddTokenModule.State.Fetched)?.customCoins ?: return
        val account = accountManager.activeAccount ?: return

        val platformCoins = customCoins.map { customCoin ->
            val coinType = customCoin.type
            val coinUid = coinType.customCoinUid
            PlatformCoin(
                Platform(coinType, customCoin.decimals, coinUid),
                Coin(coinUid, customCoin.name, customCoin.code)
            )
        }

        val wallets = platformCoins.map { Wallet(it, account) }
        walletManager.save(wallets)
    }

    fun onCleared() {
        disposable?.dispose()
    }

    private fun joinedCustomTokensSingle(
        services: List<IAddTokenBlockchainService>,
        reference: String
    ): Single<List<CustomCoin>> {
        val singles: List<Single<Optional<CustomCoin>>> = services.map { service ->
            service.customCoinsSingle(reference)
                .map { Optional.of(it) }
                .onErrorReturn { Optional.empty<CustomCoin>() }
        }

        return Single.zip(singles) { array ->
            val customTokens = array.map { it as? Optional<CustomCoin> }
            customTokens.mapNotNull { it?.orElse(null) }
        }
    }

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }

}
