package io.horizontalsystems.bankwallet.modules.transactions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Wallet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<Wallet?>>()
    val transactionViewItemLiveEvent = SingleLiveEvent<TransactionViewItem>()
    val reloadChangeEvent = SingleLiveEvent<DiffUtil.DiffResult>()
    val reloadLiveEvent = SingleLiveEvent<Unit>()
    val reloadItemsLiveEvent = SingleLiveEvent<List<Int>>()
    val addItemsLiveEvent = SingleLiveEvent<Pair<Int, Int>>()

    private var flushSubject = PublishSubject.create<Unit>()
    private var indexesToUpdate = mutableListOf<Int>()
    private val disposables = CompositeDisposable()

    fun init() {
        TransactionsModule.initModule(this, this)
        delegate.viewDidLoad()

        flushSubject
                .debounce(200, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { _ -> reloadWithBuffer() }
                .subscribe()?.let { disposables.add(it) }
    }

    override fun showFilters(filters: List<Wallet?>) {
        filterItems.postValue(filters)
    }

    override fun reload() {
        reloadLiveEvent.postValue(Unit)
    }

    override fun reloadChange(diff: DiffUtil.DiffResult) {
        reloadChangeEvent.postValue(diff)
    }

    override fun reloadItems(updatedIndexes: List<Int>) {
        indexesToUpdate.addAll(updatedIndexes)
        indexesToUpdate = indexesToUpdate.distinct().toMutableList()
        flushSubject.onNext(Unit)
    }

    private fun reloadWithBuffer() {
        reloadItemsLiveEvent.value = indexesToUpdate
        indexesToUpdate.clear()
    }

    override fun addItems(fromIndex: Int, count: Int) {
        if (fromIndex == 0) {
            reload()
        } else {
            addItemsLiveEvent.postValue(Pair(fromIndex, count))
        }
    }

    override fun openTransactionInfo(transactionViewItem: TransactionViewItem) {
        transactionViewItemLiveEvent.postValue(transactionViewItem)
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
