package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.customCoinUid
import io.horizontalsystems.bankwallet.modules.addtoken.AddTokenModule.IAddTokenBlockchainService
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Platform
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AddTokenService(
    private val coinManager: ICoinManager,
    private val blockchainServices: List<IAddTokenBlockchainService>,
    private val walletManager: IWalletManager,
    private val accountManager: IAccountManager
) {

    private val _stateFlow =
        MutableSharedFlow<Result<AddTokenModule.State>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val stateFlow = _stateFlow.asSharedFlow()

    private var disposable: Disposable? = null

    var state: AddTokenModule.State = AddTokenModule.State.Idle
        private set(value) {
            field = value
            _stateFlow.tryEmit(Result.success(value))
        }

    private var fetchCustomCoinsJob: Job? = null

    suspend fun set(reference: String?) = withContext(Dispatchers.IO) {
        fetchCustomCoinsJob?.cancel()

        val referenceNonNull = if (reference.isNullOrEmpty()) {
            state = AddTokenModule.State.Idle
            return@withContext
        } else {
            reference
        }

        val validServices = blockchainServices.filter { it.isValid(referenceNonNull) }

        if (validServices.isEmpty()) {
            state = AddTokenModule.State.Failed(TokenError.InvalidReference)
            return@withContext
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
            return@withContext
        }

        state = AddTokenModule.State.Loading

        fetchCustomCoinsJob = launch {
            val customCoins = validServices.mapNotNull { service ->
                try {
                    ensureActive()
                    service.customCoin(reference)
                } catch (e: Exception) {
                    null
                }
            }

            ensureActive()
            state = if (customCoins.isEmpty()) {
                AddTokenModule.State.Failed(TokenError.NotFound)
            } else {
                AddTokenModule.State.Fetched(customCoins)
            }
        }
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

    sealed class TokenError : Exception() {
        object InvalidReference : TokenError()
        object NotFound : TokenError()
    }

}
