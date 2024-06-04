package io.horizontalsystems.bankwallet.modules.coin.overview

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.FullCoin
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.net.URL

class CoinOverviewService(
    val fullCoin: FullCoin,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val appConfigProvider: AppConfigProvider,
    private val languageManager: LanguageManager
) {
    val currency get() = currencyManager.baseCurrency

    private var job: Job? = null
    private val coinOverviewSubject = BehaviorSubject.create<DataState<CoinOverviewItem>>()
    val coinOverviewObservable: Observable<DataState<CoinOverviewItem>>
        get() = coinOverviewSubject

    private val guideUrls = mapOf(
        "bitcoin" to "guides/token_guides/en/bitcoin.md",
        "ethereum" to "guides/token_guides/en/ethereum.md",
        "bitcoin-cash" to "guides/token_guides/en/bitcoin-cash.md",
        "zcash" to "guides/token_guides/en/zcash.md",
        "uniswap" to "guides/token_guides/en/uniswap.md",
        "curve-dao-token" to "guides/token_guides/en/curve-finance.md",
        "balancer" to "guides/token_guides/en/balancer-dex.md",
        "synthetix-network-token" to "guides/token_guides/en/synthetix.md",
        "tether" to "guides/token_guides/en/tether.md",
        "maker" to "guides/token_guides/en/makerdao.md",
        "dai" to "guides/token_guides/en/makerdao.md",
        "aave" to "guides/token_guides/en/aave.md",
        "compound" to "guides/token_guides/en/compound.md",
    )

    private val guideUrl: String?
        get() = guideUrls[fullCoin.coin.uid]?.let { URL(URL(appConfigProvider.guidesUrl), it).toString() }

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        fetchCoinOverview()
    }

    private fun fetchCoinOverview() {
        job = coroutineScope.launch {
            try {
                val marketInfoOverview = marketKit.marketInfoOverviewSingle(
                    fullCoin.coin.uid,
                    currencyManager.baseCurrency.code,
                    languageManager.currentLanguage
                ).await()
                coinOverviewSubject.onNext(
                    DataState.Success(
                        CoinOverviewItem(
                            fullCoin.coin.code,
                            marketInfoOverview,
                            guideUrl
                        )
                    )
                )
            } catch (e: Throwable) {
                coinOverviewSubject.onNext(DataState.Error(e))
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        job?.cancel()
        start()
    }
}
