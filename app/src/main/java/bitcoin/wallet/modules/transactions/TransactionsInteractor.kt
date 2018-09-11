package bitcoin.wallet.modules.transactions

import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.core.ExchangeRateManager
import bitcoin.wallet.entities.CoinValue
import bitcoin.wallet.entities.CurrencyValue
import bitcoin.wallet.entities.DollarCurrency
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem.Status.PENDING
import bitcoin.wallet.modules.transactions.TransactionRecordViewItem.Status.SUCCESS
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class TransactionsInteractor(private val adapterManager: AdapterManager, private val exchangeRateManager: ExchangeRateManager) : TransactionsModule.IInteractor {

    var delegate: TransactionsModule.IInteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun retrieveFilters() {
        adapterManager.subject.subscribe {
            disposables.clear()
            initialFetchAndSubscribe()
        }

        initialFetchAndSubscribe()
    }

    private fun initialFetchAndSubscribe() {
        val filters = adapterManager.adapters.map {
            TransactionFilterItem(it.id, it.coin.name)
        }
        delegate?.didRetrieveFilters(filters)

        adapterManager.adapters.forEach { adapter ->
            disposables.add(adapter.transactionRecordsSubject.subscribe {
                retrieveTransactionItems()
            })
        }
    }

    override fun retrieveTransactionItems(adapterId: String?) {
        val rates = exchangeRateManager.exchangeRates
        val items = mutableListOf<TransactionRecordViewItem>()

        val filteredAdapters = adapterManager.adapters.filter { adapterId == null || it.id == adapterId }

        filteredAdapters.forEach { adapter ->
            val latestBlockHeight = adapter.latestBlockHeight
            adapter.transactionRecords.forEach { record ->
                val confirmations = record.blockHeight?.let { latestBlockHeight - it + 1 } ?: 0
                val convertedValue = rates[adapter.coin.code]?.let { it * record.amount }

                val item = TransactionRecordViewItem(
                        hash = record.transactionHash,
                        amount = CoinValue(adapter.coin, record.amount.toDouble()),
                        currencyAmount = convertedValue?.let { CurrencyValue(currency = DollarCurrency(), value = it) },
                        fee = CoinValue(coin = adapter.coin, value = record.fee.toDouble()),
                        from = record.from.first(),
                        to = record.to.first(),
                        incoming = record.amount > 0,
                        blockHeight = record.blockHeight,
                        date = record.timestamp?.let { Date(it) },
                        status = if (confirmations > 0) SUCCESS else PENDING,
                        confirmations = confirmations
                )
                items.add(item)
            }
        }

        delegate?.didRetrieveItems(items)
    }

}
