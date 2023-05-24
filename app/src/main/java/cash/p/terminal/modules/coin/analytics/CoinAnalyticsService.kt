package cash.p.terminal.modules.coin.analytics

import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.core.subscribeIO
import cash.p.terminal.entities.Currency
import cash.p.terminal.entities.DataState
import io.horizontalsystems.marketkit.models.Analytics
import io.horizontalsystems.marketkit.models.AnalyticsPreview
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.TokenType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import retrofit2.HttpException

class CoinAnalyticsService(
    val fullCoin: FullCoin,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) {
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<DataState<AnalyticData>>()
    val stateObservable: Observable<DataState<AnalyticData>>
        get() = stateSubject

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

    fun blockchains(uids: List<String>): List<Blockchain> {
        return marketKit.blockchains(uids)
    }

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }

    private fun fetch() {
        val authToken = ""
        marketKit.analyticsSingle(fullCoin.coin.uid, currency.code, authToken)
            .subscribeIO({ item ->
                stateSubject.onNext(DataState.Success(AnalyticData(analytics = item)))
            }, {
                handleError(it)
            }).let {
                disposables.add(it)
            }
    }

    private fun handleError(error: Throwable) {
        if (error is HttpException && error.code() == 401) {
            val addresses = listOf<String>()
            marketKit.analyticsPreviewSingle(fullCoin.coin.uid, addresses)
                .subscribeIO({ item ->
                    stateSubject.onNext(DataState.Success(AnalyticData(analyticsPreview = item)))
                }, {
                    stateSubject.onNext(DataState.Error(it))
                }).let {
                    disposables.add(it)
                }
        } else {
            stateSubject.onNext(DataState.Error(error))
        }
    }

    data class AnalyticData(
        val analytics: Analytics? = null,
        val analyticsPreview: AnalyticsPreview? = null
    )

}
