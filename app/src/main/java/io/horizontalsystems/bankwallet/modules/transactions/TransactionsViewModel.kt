package io.horizontalsystems.bankwallet.modules.transactions

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.v7.util.DiffUtil
import io.horizontalsystems.bankwallet.SingleLiveEvent
import io.horizontalsystems.bankwallet.entities.Coin
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class TransactionsViewModel : ViewModel(), TransactionsModule.IView, TransactionsModule.IRouter {

    lateinit var delegate: TransactionsModule.IViewDelegate

    val filterItems = MutableLiveData<List<Coin?>>()
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
                .doOnNext { unit -> reloadWithBuffer() }
                .subscribe()?.let { disposables.add(it) }
    }

    override fun showFilters(filters: List<Coin?>) {
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
        transactionViewItemLiveEvent.value = transactionViewItem
    }

    override fun onCleared() {
        delegate.onClear()
    }
}
