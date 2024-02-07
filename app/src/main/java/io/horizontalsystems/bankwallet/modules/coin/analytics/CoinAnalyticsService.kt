package io.horizontalsystems.bankwallet.modules.coin.analytics

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.InvalidAuthTokenException
import io.horizontalsystems.bankwallet.core.NoAuthTokenException
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SubscriptionManager
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.rx2.await

class CoinAnalyticsService(
    val fullCoin: FullCoin,
    private val apiTag: String,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val subscriptionManager: SubscriptionManager,
    private val accountManager: IAccountManager,
) {

    private val _stateFlow = MutableStateFlow<DataState<AnalyticData>>(DataState.Loading)
    val stateFlow: Flow<DataState<AnalyticData>> = _stateFlow

    val currency: Currency
        get() = currencyManager.baseCurrency

    val auditAddresses: List<String> by lazy {
        fullCoin.tokens.mapNotNull { token ->
            val tokenQuery = token.tokenQuery
            when (val tokenType = tokenQuery.tokenType) {
                is TokenType.Eip20 -> when (tokenQuery.blockchainType) {
                    BlockchainType.Ethereum -> tokenType.address
                    BlockchainType.BinanceSmartChain -> tokenType.address
                    else -> null
                }
                else -> null
            }
        }
    }

    fun blockchain(uid: String): Blockchain? {
        return marketKit.blockchain(uid)
    }

    fun blockchains(uids: List<String>): List<Blockchain> {
        return marketKit.blockchains(uids)
    }

    suspend fun start() {
        subscriptionManager.authTokenFlow.collect {
            fetch()
        }
    }

    suspend fun refresh() {
        fetch()
    }

    private suspend fun fetch() {
        if (!subscriptionManager.hasSubscription()) {
            preview()
        } else {
            _stateFlow.emit(DataState.Loading)

            try {
                marketKit.analyticsSingle(fullCoin.coin.uid, currency.code, apiTag).await()
                    .let {
                        _stateFlow.emit(DataState.Success(AnalyticData(analytics = it)))
                    }
            } catch (error: Throwable) {
                handleError(error)
            }
        }
    }

    private suspend fun handleError(error: Throwable) {
        when (error) {
            is NoAuthTokenException,
            is InvalidAuthTokenException -> {
                preview()
            }

            else -> {
                _stateFlow.emit(DataState.Error(error))
            }
        }
    }

    private suspend fun preview() {
        val addresses = accountManager.accounts.mapNotNull {
            it.type.evmAddress(App.evmBlockchainManager.getChain(BlockchainType.Ethereum))?.hex
        }

        try {
            marketKit.analyticsPreviewSingle(fullCoin.coin.uid, addresses, apiTag).await()
                .let {
                    _stateFlow.emit(DataState.Success(AnalyticData(analyticsPreview = it)))
                }
        } catch (error: Throwable) {
            _stateFlow.emit(DataState.Error(error))
        }
    }

    data class AnalyticData(
        val analytics: Analytics? = null,
        val analyticsPreview: AnalyticsPreview? = null
    )

}
