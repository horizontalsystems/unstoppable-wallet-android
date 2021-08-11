package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.balance.BalanceActiveWalletRepository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.util.*
import java.util.concurrent.Executors

class Transactions2Service(
    private val balanceActiveWalletRepository: BalanceActiveWalletRepository,
    private val transactionRecordRepository: TransactionRecordRepository
) : Clearable {

    private val filterCoinsSubject = BehaviorSubject.create<List<Wallet>>()
    val filterCoinsObservable: Observable<List<Wallet>> = filterCoinsSubject

    private val filterCoinSubject = BehaviorSubject.createDefault<Optional<Wallet>>(Optional.empty())
    val filterCoinObservable: Observable<Optional<Wallet>> = filterCoinSubject

    private val itemsSubject = BehaviorSubject.create<List<TransactionItem>>()
    val itemsObservable: Observable<List<TransactionItem>> get() = itemsSubject

    val syncingObservable: Observable<Boolean> = Observable.just(true)

    private val disposables = CompositeDisposable()

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
                itemsSubject.onNext(it.map {
                    TransactionItem(it, null, null)
                })
            }
            .let {
                disposables.add(it)
            }
    }

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
}
