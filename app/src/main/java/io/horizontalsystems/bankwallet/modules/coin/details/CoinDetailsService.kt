package io.horizontalsystems.bankwallet.modules.coin.details

import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.profeatures.ProFeaturesAuthorizationManager
import io.horizontalsystems.bankwallet.modules.profeatures.ProNft
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class CoinDetailsService(
    private val fullCoin: FullCoin,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: ICurrencyManager,
    private val proFeaturesAuthorizationManager: ProFeaturesAuthorizationManager
) {
    private val disposables = CompositeDisposable()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val stateSubject = BehaviorSubject.create<DataState<Item>>()
    val stateObservable: Observable<DataState<Item>>
        get() = stateSubject

    val usdCurrency: Currency
        get() {
            val currencies = currencyManager.currencies
            return currencies.first { it.code == "USD" }
        }

    val currency: Currency
        get() = currencyManager.baseCurrency

    val hasMajorHolders: Boolean by lazy { fullCoin.tokens.any { it.tokenQuery.blockchainType is BlockchainType.Ethereum && it.tokenQuery.tokenType is TokenType.Eip20 } }

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

    val coin = fullCoin.coin

    private fun fetchCharts(details: MarketInfoDetails): Single<Item> {
        val tvlsSingle: Single<List<ChartPoint>> = if (details.tvl != null) {
            marketKit.marketInfoTvlSingle(fullCoin.coin.uid, currency.code, HsTimePeriod.Month1)
        } else {
            Single.just(listOf())
        }

        val volumeSingle = marketKit.chartInfoSingle(fullCoin.coin.uid, currency.code, HsTimePeriod.Month1)
            .map { chartInfo ->
                chartInfo.points
                    .filter { it.timestamp >= chartInfo.startTimestamp }
                    .mapNotNull { point ->
                        point.extra[ChartPointType.Volume]?.let { volume ->
                            ChartPoint(volume, point.timestamp, emptyMap())
                        }
                    }
            }

        return Single.zip(
            tvlsSingle.onErrorReturn { listOf() },
            volumeSingle.onErrorReturn { listOf() },
            Single.just(ProCharts.forbidden)
        ) { t1, t2, t3 -> Triple(t1, t2, t3) }.map { (tvls, totalVolumes, proFeatures) ->
            Item(details, tvls, totalVolumes, proFeatures)
        }
    }

    private fun proFeatures(coinUid: String, currencyCode: String): Single<ProCharts> {
        val sessionKey = proFeaturesAuthorizationManager.getSessionKey(ProNft.YAK) ?: return Single.just(ProCharts.forbidden)

        val dexVolumeSingle = marketKit
            .dexVolumesSingle(coinUid, currencyCode, HsTimePeriod.Month1, sessionKey.key.value)
            .onErrorReturn { DexVolumesResponse(listOf(), listOf()) }

        val dexLiquiditySingle = marketKit
            .dexLiquiditySingle(coinUid, currencyCode, HsTimePeriod.Month1, sessionKey.key.value)
            .onErrorReturn { DexLiquiditiesResponse(listOf(), listOf()) }

        val transactionDataSingle = marketKit
            .transactionDataSingle(coinUid, currencyCode, HsTimePeriod.Month1, null, sessionKey.key.value)
            .onErrorReturn { TransactionsDataResponse(listOf(), listOf()) }

        val activeAddressesSingle = marketKit
            .activeAddressesSingle(coinUid, currencyCode, HsTimePeriod.Month1, sessionKey.key.value)
            .onErrorReturn { ActiveAddressesDataResponse(listOf(), listOf()) }

        return Single.zip(
            dexVolumeSingle, dexLiquiditySingle, transactionDataSingle, activeAddressesSingle
        ) { dexVolumeResponse, dexLiquidityResponse, transactionDataResponse, activeAddressesResponse ->
            val dexVolumeChartPoints = dexVolumeResponse.volumePoints
            val dexLiquidityChartPoints = dexLiquidityResponse.volumePoints
            val txCountChartPoints = transactionDataResponse.countPoints
            val txVolumeChartPoints = transactionDataResponse.volumePoints
            val activeAddresses = activeAddressesResponse.countPoints

            return@zip ProCharts(
                true,
                if (dexVolumeChartPoints.isEmpty()) ProData.Empty else ProData.Completed(dexVolumeChartPoints),
                if (dexLiquidityChartPoints.isEmpty()) ProData.Empty else ProData.Completed(dexLiquidityChartPoints),
                if (txCountChartPoints.isEmpty()) ProData.Empty else ProData.Completed(txCountChartPoints),
                if (txVolumeChartPoints.isEmpty()) ProData.Empty else ProData.Completed(txVolumeChartPoints),
                if (activeAddresses.isEmpty()) ProData.Empty else ProData.Completed(activeAddresses),
            )
        }
    }

    private fun fetch() {
        marketKit.marketInfoDetailsSingle(fullCoin.coin.uid, currency.code)
            .flatMap { details ->
                fetchCharts(details)
            }
            .subscribeIO({ item ->
                stateSubject.onNext(DataState.Success(item))
            }, {
                stateSubject.onNext(DataState.Error(it))
            }).let {
                disposables.add(it)
            }
    }

    fun start() {
        fetch()

        proFeaturesAuthorizationManager.sessionKeyFlow.collectWith(scope) { sessionKey ->
            if (sessionKey?.nftName == ProNft.YAK.keyName) {
                stateSubject.onNext(DataState.Loading)
                fetch()
            }
        }
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }

    data class Item(
        val marketInfoDetails: MarketInfoDetails,
        val tvls: List<ChartPoint>?,
        val totalVolumes: List<ChartPoint>?,
        val proCharts: ProCharts
    )

    sealed class ProData {
        object Empty : ProData()
        object Forbidden : ProData()
        class Completed(val chartPoints: List<ChartPoint>) : ProData()
    }

    data class ProCharts(
        val activated: Boolean,
        val dexVolumes: ProData,
        val dexLiquidity: ProData,
        val txCount: ProData,
        val txVolume: ProData,
        val activeAddresses: ProData
    ) {
        companion object {
            val forbidden = ProCharts(false, ProData.Forbidden, ProData.Forbidden, ProData.Forbidden, ProData.Forbidden, ProData.Forbidden)
        }
    }

}
