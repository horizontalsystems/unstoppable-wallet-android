package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.IExchangeRateManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.Currency
import bitcoin.wallet.entities.CurrencyValue
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

class TransactionsInteractor(
        private val adapterManager: AdapterManager,
        private val exchangeRateManager: IExchangeRateManager,
        private val baseCurrencyFlowable: Flowable<Currency>) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()
    private var adapterManagerDisposable: Disposable? = null
    private var clickedAdapterId: String? = null

    override fun retrieveFilters() {
        adapterManagerDisposable = adapterManager.subject.subscribe {
            disposables.clear()
            initialFetchAndSubscribe()
        }

        initialFetchAndSubscribe()
    }

    private fun initialFetchAndSubscribe() {
        val adapters = adapterManager.adapters
        val filters: List<TransactionFilterItem> = adapters.map {
            TransactionFilterItem(it.id, it.coin.name)
        }
        delegate?.didRetrieveFilters(filters)

        adapterManager.adapters.forEach { adapter ->
            disposables.add(adapter.transactionRecordsSubject.subscribe {
                retrieveTransactions(clickedAdapterId)
            })
        }

        retrieveTransactions(clickedAdapterId)
    }

    override fun retrieveTransactions(adapterId: String?) {
        clickedAdapterId = adapterId
        disposables.add(baseCurrencyFlowable.subscribe { baseCurrency ->
            retrieveTransactionItemsWithBaseCurrency(baseCurrency, clickedAdapterId)
        })
    }

    override fun onCleared() {
        disposables.clear()
        adapterManagerDisposable?.dispose()
    }

    private fun retrieveTransactionItemsWithBaseCurrency(baseCurrency: Currency, adapterId: String?) {
        val items = mutableListOf<TransactionRecordViewItem>()

        val filteredAdapters = adapterManager.adapters.filter { adapterId == null || it.id == adapterId }
        val flowableList = mutableListOf<Flowable<Pair<String, Double>>>()

        filteredAdapters.forEach { adapter ->
            adapter.transactionRecords.forEach { record ->

                val item = TransactionRecordViewItem(
                        hash = record.transactionHash,
                        adapterId = adapter.id,
                        amount = CoinValue(adapter.coin, record.amount),
                        fee = CoinValue(coin = adapter.coin, value = record.fee),
                        from = record.from.first(),
                        to = record.to.first(),
                        incoming = record.amount > 0,
                        blockHeight = record.blockHeight,
                        date = record.timestamp?.let { Date(it) },
                        status = record.status
                )
                items.add(item)
                record.timestamp?.let { timestamp ->
                    flowableList.add(
                            exchangeRateManager.getRate(coinCode = record.coinCode, currency = baseCurrency.code, timestamp = timestamp)
                                    .map { Pair(record.transactionHash, it) }
                    )
                }
            }
        }

        items.sortByDescending { it.date }

        disposables.add(Flowable.zip(flowableList, Arrays::asList)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(io.reactivex.android.schedulers.AndroidSchedulers.mainThread())
                .map { resultRates ->
                    (resultRates as List<Pair<String, Double>>).toMap()
                }
                .subscribe { ratesMap ->
                    items.forEach { item ->
                        val rate = ratesMap[item.hash] ?: 0.0
                        if (rate > 0) {
                            val value = item.amount.value * rate
                            item.currencyAmount = CurrencyValue(currency = baseCurrency, value = value)
                            item.exchangeRate = rate
                        }
                    }

                    delegate?.didRetrieveItems(items)
                })
    }

}
