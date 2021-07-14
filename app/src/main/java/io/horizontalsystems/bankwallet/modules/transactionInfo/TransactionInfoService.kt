package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class TransactionInfoService(
    private val adapter: ITransactionsAdapter,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    buildConfigProvider: IBuildConfigProvider,
    private val accountSettingManager: AccountSettingManager
) : Clearable {

    private val disposables = CompositeDisposable()
    val ratesAsync = BehaviorSubject.create<Map<Coin, CurrencyValue>>()

    val lastBlockInfo: LastBlockInfo?
        get() = adapter.lastBlockInfo

    val testMode = buildConfigProvider.testMode

    override fun clear() {
        disposables.clear()
    }

    fun getRates(coins: List<Coin>, timestamp: Long) {
        val flowables: List<Single<Pair<Coin, CurrencyValue>>> = coins.map { coin ->
            xRateManager.historicalRate(coin.type, currencyManager.baseCurrency.code, timestamp)
                .onErrorResumeNext(Single.just(BigDecimal.ZERO)) //provide default value on error
                .map {
                    Pair(coin, CurrencyValue(currencyManager.baseCurrency, it))
                }
        }

        Single.zip(flowables) { array ->
            array.mapNotNull {
                it as Pair<Coin, CurrencyValue>
                if (it.second.value == BigDecimal.ZERO) {
                    null
                } else {
                    it.first to it.second
                }
            }.toMap()
        }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { rates, _ ->
                ratesAsync.onNext(rates)
            }
            .let { disposables.add(it) }
    }

    fun getRaw(transactionHash: String): String? {
        return adapter.getRawTransaction(transactionHash)
    }

    fun ethereumNetworkType(account: Account): EthereumKit.NetworkType {
        return accountSettingManager.ethereumNetwork(account).networkType
    }

}
