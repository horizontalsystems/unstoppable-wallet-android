package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.providers.FeeCoinProvider
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BalanceItemXRateRepository(
    private val itemRepository: ItemRepository<BalanceModule.BalanceItem>,
    private val xRateManager: IRateManager,
    private val currencyManager: ICurrencyManager,
    private val feeCoinProvider: FeeCoinProvider
) : ItemRepository<BalanceModule.BalanceItem> {

    private var balanceItems = listOf<BalanceModule.BalanceItem>()
    private val disposables = CompositeDisposable()
    private val latestRatesDisposables = CompositeDisposable()

    private val itemsSubject = BehaviorSubject.create<List<BalanceModule.BalanceItem>>()

    override val itemsObservable: Observable<List<BalanceModule.BalanceItem>>
        get() = itemsSubject
            .doOnSubscribe {
                subscribeForUpdates()
            }
            .doFinally {
                unsubscribeFromUpdates()
            }

    override fun refresh() {
        xRateManager.refresh(baseCurrency.code)
        itemRepository.refresh()
    }

    private val baseCurrency: Currency
        get() = currencyManager.baseCurrency

    private fun unsubscribeFromUpdates() {
        disposables.clear()
        latestRatesDisposables.clear()
    }

    private fun subscribeForUpdates() {
        itemRepository.itemsObservable
            .subscribeIO {
                balanceItems = it

                reset()
            }
            .let {
                disposables.add(it)
            }

        currencyManager.baseCurrencyUpdatedSignal
            .subscribeIO {
                reset()
            }
            .let {
                disposables.add(it)
            }


    }

    private fun reset() {
        unsubscribeFromLatestRateUpdates()

        setLatestRates()
        emitBalanceItems()

        subscribeForLatestRateUpdates()
    }

    private fun emitBalanceItems() {
        itemsSubject.onNext(balanceItems)
    }

    private fun unsubscribeFromLatestRateUpdates() {
        latestRatesDisposables.clear()
    }

    private fun setLatestRates() {
        balanceItems.forEach { balanceItem ->
            balanceItem.latestRate = xRateManager.latestRate(balanceItem.wallet.coin.type, baseCurrency.code)
        }
    }

    private fun subscribeForLatestRateUpdates() {
        val coinTypes = balanceItems.map { it.wallet.coin.type }

        // the send module needs the fee coin rate synchronous
        // that is why here we request fee coins too
        // todo: need to find a better solution
        val feeCoinTypes = coinTypes.mapNotNull { feeCoinProvider.feeCoinType(it) }
        val allCoinTypes = (coinTypes + feeCoinTypes).distinct()

        xRateManager.latestRateObservable(allCoinTypes, baseCurrency.code)
            .subscribeIO { latestRates: Map<CoinType, LatestRate> ->
                balanceItems.forEach { balanceItem ->
                    latestRates[balanceItem.wallet.coin.type]?.let {
                        balanceItem.latestRate = it
                    }
                }

                emitBalanceItems()
            }
            .let {
                latestRatesDisposables.add(it)
            }
    }
}
