package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.balance.BalanceActiveWalletRepository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class Transactions2Service(
    private val balanceActiveWalletRepository: BalanceActiveWalletRepository,
    private val transactionRecordRepository: ITransactionRecordRepository,
    private val xRateRepository: TransactionsXRateRepository
) : Clearable {

    private val filterCoinsSubject = BehaviorSubject.create<List<Wallet>>()
    val filterCoinsObservable: Observable<List<Wallet>> = filterCoinsSubject

    private val filterCoinSubject = BehaviorSubject.createDefault<Optional<Wallet>>(Optional.empty())
    val filterCoinObservable: Observable<Optional<Wallet>> = filterCoinSubject

    private val itemsSubject = BehaviorSubject.create<List<TransactionItem>>()
    val itemsObservable: Observable<List<TransactionItem>> get() = itemsSubject

    val syncingObservable: Observable<Boolean> = Observable.just(true)

    private val disposables = CompositeDisposable()
    private val transactionItems = CopyOnWriteArrayList<TransactionItem>()

    init {
        balanceActiveWalletRepository.itemsObservable
            .subscribeIO { wallets ->
                handleUpdatedWallets(wallets)
            }
            .let {
                disposables.add(it)
            }

        transactionRecordRepository.itemsObservable
            .subscribeIO {
                handleUpdatedRecords(it)
            }
            .let {
                disposables.add(it)
            }

        xRateRepository.dataExpiredObservable
            .subscribeIO {
                handleUpdatedHistoricalRates()
            }
            .let {
                disposables.add(it)
            }

        xRateRepository.historicalRateObservable
            .subscribeIO {
                handleUpdatedHistoricalRate(it.first, it.second)
            }
            .let {
                disposables.add(it)
            }
    }

    @Synchronized
    private fun handleUpdatedHistoricalRate(key: HistoricalRateKey, rate: CurrencyValue) {
        for (i in 0 until transactionItems.size) {
            val item = transactionItems[i]

            item.record.mainValue?.let { mainValue ->
                if (mainValue.coin.type == key.coinType && item.record.timestamp == key.timestamp) {
                    val currencyValue = CurrencyValue(rate.currency, mainValue.value * rate.value)

                    transactionItems[i] = item.copy(xxxCurrencyValue = currencyValue)
                }
            }
        }

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedHistoricalRates() {
        for (i in 0 until transactionItems.size) {
            val item = transactionItems[i]

            val currencyValue = item.record.mainValue?.let { mainValue ->
                xRateRepository.getHistoricalRate(HistoricalRateKey(mainValue.coin.type, item.record.timestamp))?.let { rate ->
                    CurrencyValue(rate.currency, mainValue.value * rate.value)
                }
            }

            transactionItems[i] = item.copy(xxxCurrencyValue = currencyValue)
        }

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedRecords(transactionRecords: List<TransactionRecord>) {
        transactionItems.clear()

        transactionRecords.forEach { record ->
            val currencyValue = record.mainValue?.let { mainValue ->
                xRateRepository.getHistoricalRate(HistoricalRateKey(mainValue.coin.type, record.timestamp))?.let { rate ->
                    CurrencyValue(rate.currency, mainValue.value * rate.value)
                }
            }

            transactionItems.add(TransactionItem(record, currencyValue, null))
        }

        itemsSubject.onNext(transactionItems)
    }

    @Synchronized
    private fun handleUpdatedWallets(wallets: List<Wallet>) {
        filterCoinsSubject.onNext(wallets)
        transactionRecordRepository.setWallets(wallets)
    }

    override fun clear() {
        disposables.clear()
    }

    private val executorService = Executors.newCachedThreadPool()

    fun setFilterCoin(w: Wallet?) {
        executorService.submit {
            filterCoinSubject.onNext(w?.let { Optional.of(it) } ?: Optional.empty())
            transactionRecordRepository.setSelectedWallet(w)
        }
    }

    fun loadNext() {
        executorService.submit {
            transactionRecordRepository.loadNext()
        }
    }

    fun fetchRateIfNeeded(recordUid: String) {
        executorService.submit {
            transactionItems.find { it.record.uid == recordUid }?.let { transactionItem ->
                if (transactionItem.xxxCurrencyValue == null) {
                    transactionItem.record.mainValue?.let { mainValue ->
                        xRateRepository.fetchHistoricalRate(HistoricalRateKey(mainValue.coin.type, transactionItem.record.timestamp))
                    }
                }
            }
        }
    }
}
