package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IAddTokenBlockchainService
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.bankwallet.entities.Wallet
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
            .subscribe({ customTokens ->
                state = if (customTokens.isEmpty()) {
                    AddTokenModule.State.Failed(TokenError.NotFound)
                } else {
                    AddTokenModule.State.Fetched(customTokens)
                }
            }, { error ->
                state = AddTokenModule.State.Failed(error)
            })
    }

    fun save() {
        val customTokens = (state as? AddTokenModule.State.Fetched)?.customTokens ?: return
        coinManager.save(customTokens)

        val account = accountManager.activeAccount ?: return
        val wallets = customTokens.map { customToken -> Wallet(customToken.platformCoin, account) }

        walletManager.save(wallets)
    }

    fun onCleared() {
        disposable?.dispose()
    }

    private fun joinedCustomTokensSingle(
        services: List<IAddTokenBlockchainService>,
        reference: String
    ): Single<List<CustomToken>> {
        val singles: List<Single<Optional<CustomToken>>> = services.map { service ->
            service.customTokenAsync(reference)
                .map { Optional.of(it) }
                .onErrorReturn { Optional.empty<CustomToken>() }
        }

        return Single.zip(singles) { array ->
            val customTokens = array.map { it as? Optional<CustomToken> }
            customTokens.mapNotNull { it?.orElse(null) }
        }
    }

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }

}
