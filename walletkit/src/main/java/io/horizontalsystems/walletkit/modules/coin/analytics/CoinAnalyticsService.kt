package io.horizontalsystems.walletkit.modules.coin.analytics

import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.InvalidAuthTokenException
import io.horizontalsystems.walletkit.core.NoAuthTokenException
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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
            val analytics = marketKit.analyticsSingle(fullCoin.coin.uid, currency.code)
            _stateFlow.emit(DataState.Success(AnalyticData(analytics = analytics)))
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
        // Only the active account. Sending every account's address in one request tells the server
        // that all of those wallets belong to the same person, which is a correlation the user
        // never asked for and cannot undo.
        val addresses = listOfNotNull(
            accountManager.activeAccount?.let { account ->
                ChainRegistry.all.firstNotNullOfOrNull { it.analyticsAddress(account.type) }
            }
        )

        try {
            val preview = marketKit.analyticsPreviewSingle(fullCoin.coin.uid, addresses)
            _stateFlow.emit(DataState.Success(AnalyticData(analyticsPreview = preview)))
        } catch (error: Throwable) {
            _stateFlow.emit(DataState.Error(error))
        }
    }

    data class AnalyticData(
        val analytics: Analytics? = null,
        val analyticsPreview: AnalyticsPreview? = null
    )

}
