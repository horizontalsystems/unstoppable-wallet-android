package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class TransactionsViewModel(
    private val service: TransactionsService,
    private val transactionViewItem2Factory: TransactionViewItemFactory
) : ViewModel() {

    lateinit var tmpItemToShow: TransactionItem

    val syncingLiveData = MutableLiveData<Boolean>()
    val filterCoinsLiveData = MutableLiveData<List<Filter<TransactionWallet>>>()
    val filterTypesLiveData = MutableLiveData<List<Filter<FilterTransactionType>>>()
    val transactionList = MutableLiveData<ItemsList>()

    private val disposables = CompositeDisposable()

    init {
        service.syncingObservable
            .subscribeIO {
                syncingLiveData.postValue(it)
            }
            .let {
                disposables.add(it)
            }

        service.typesObservable
            .subscribeIO { (types, selectedType) ->
                val filterTypes = types.map {
                    Filter(it, it == selectedType)
                }
                filterTypesLiveData.postValue(filterTypes)
            }
            .let {
                disposables.add(it)
            }

        service.walletsObservable
            .subscribeIO { (wallets, selected) ->
                val filterCoins = wallets.map {
                    Filter(it, it == selected)
                }
                filterCoinsLiveData.postValue(filterCoins)
            }
            .let {
                disposables.add(it)
            }

        service.itemsObservable
            .subscribeIO {
                val transactionList = when {
                    it.isNotEmpty() -> {
                        val viewItems = it.map { transactionViewItem2Factory.convertToViewItemCached(it) }
                        ItemsList.Filled(viewItems, generateHeaders(viewItems))
                    }
                    else -> ItemsList.Blank
                }
                this.transactionList.postValue(transactionList)
            }
            .let {
                disposables.add(it)
            }
    }

    private fun generateHeaders(viewItems: List<TransactionViewItem>): Map<Int, String> {
        val headers = mutableMapOf<Int, String>()

        var prevSectionHeader: String? = null
        viewItems.forEachIndexed { index, transactionViewItem ->
            val sectionHeader = transactionViewItem.formattedDate

            if (sectionHeader != prevSectionHeader) {
                headers[index] = sectionHeader
                prevSectionHeader = sectionHeader
            }
        }

        return headers
    }

    fun setFilterTransactionType(filterType: FilterTransactionType) {
        service.setFilterType(filterType)
    }

    fun setFilterCoin(w: TransactionWallet?) {
        service.setFilterCoin(w)
    }

    fun onBottomReached() {
        service.loadNext()
    }

    fun willShow(viewItem: TransactionViewItem) {
        service.fetchRateIfNeeded(viewItem.uid)
    }

    sealed class ItemsList(val items: List<TransactionViewItem>, val headers: Map<Int, String>) {
        object Blank : ItemsList(listOf(), mapOf())
        class Filled(items: List<TransactionViewItem>, headers: Map<Int, String>) : ItemsList(items, headers)
    }

    override fun onCleared() {
        service.clear()
    }

    fun getTransactionItem(viewItem: TransactionViewItem) = service.getTransactionItem(viewItem.uid)
}

data class TransactionItem(
    val record: TransactionRecord,
    val currencyValue: CurrencyValue?,
    val lastBlockInfo: LastBlockInfo?
) {
    val createdAt = System.currentTimeMillis()
}

data class TransactionViewItem(
    val uid: String,
    val typeIcon: Int,
    val progress: Int?,
    val title: String,
    val subtitle: String,
    val primaryValue: ColoredValue?,
    val secondaryValue: ColoredValue?,
    val date: Date,
    val sentToSelf: Boolean = false,
    val doubleSpend: Boolean = false,
    val locked: Boolean? = null
) {
    val formattedDate = formatDate(date).uppercase()

    fun itemTheSame(newItem: TransactionViewItem) = uid == newItem.uid

    fun contentTheSame(newItem: TransactionViewItem): Boolean {
        return this == newItem
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val today = Calendar.getInstance()
        if (calendar[Calendar.YEAR] == today[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Today)
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]) {
            return Translator.getString(R.string.Timestamp_Yesterday)
        }

        return DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy")
    }
}

enum class FilterTransactionType {
    All, Incoming, Outgoing, Swap, Approve
}
