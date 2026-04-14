package com.quantum.wallet.bankwallet.modules.coin.analytics

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.IAccountManager
import com.quantum.wallet.bankwallet.core.InvalidAuthTokenException
import com.quantum.wallet.bankwallet.core.NoAuthTokenException
import com.quantum.wallet.bankwallet.core.managers.CurrencyManager
import com.quantum.wallet.bankwallet.core.managers.MarketKitWrapper
import com.quantum.wallet.bankwallet.entities.Currency
import com.quantum.wallet.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.rx2.await

class CoinAnalyticsService(
    val fullCoin: FullCoin,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val accountManager: IAccountManager,
) {

    private val _stateFlow = MutableStateFlow<DataState<AnalyticData>>(DataState.Loading)
    val stateFlow: Flow<DataState<AnalyticData>> = _stateFlow

    val currency: Currency
        get() = currencyManager.baseCurrency

    fun blockchain(uid: String): Blockchain? {
        return marketKit.blockchain(uid)
    }

    fun blockchains(uids: List<String>): List<Blockchain> {
        return marketKit.blockchains(uids)
    }

    suspend fun start() {
        fetch()
        UserSubscriptionManager.activeSubscriptionStateFlow.collect {
            fetch()
        }
    }

    suspend fun refresh() {
        fetch()
    }

    private suspend fun fetch() {
        _stateFlow.emit(DataState.Loading)

        try {
            marketKit.analyticsSingle(fullCoin.coin.uid, currency.code).await()
                .let {
                    _stateFlow.emit(DataState.Success(AnalyticData(analytics = it)))
                }
        } catch (error: Throwable) {
            handleError(error)
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
            marketKit.analyticsPreviewSingle(fullCoin.coin.uid, addresses).await()
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
