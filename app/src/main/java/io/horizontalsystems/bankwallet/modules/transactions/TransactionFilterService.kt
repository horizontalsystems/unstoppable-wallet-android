package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Wallet

class TransactionFilterService {
    private var transactionWallets: List<TransactionWallet> = listOf()
    var selectedWallet: TransactionWallet? = null
    var selectedTransactionType: FilterTransactionType = FilterTransactionType.All

    fun setWallets(wallets: List<Wallet>) {
        transactionWallets = wallets.map {
            TransactionWallet(it.coin, it.transactionSource)
        }

        // Reset selectedWallet if it does not exist in the new wallets list or leave it if it does
        // When there is a coin added or removed then the selectedWallet should be left
        // When the whole wallets list is changed (switch account) it should be reset
        if (!transactionWallets.contains(selectedWallet)) {
            selectedWallet = null

            selectedTransactionType = FilterTransactionType.All
        }
    }

    fun getTransactionWallets(): List<TransactionWallet> {
        return transactionWallets
    }

    fun getFilterTypes(): List<FilterTransactionType> {
        return FilterTransactionType.values().toList()
    }


//    private val walletsSubject = BehaviorSubject.create<List<TransactionWallet>>()
//    val walletsObservable: Observable<List<TransactionWallet>> = walletsSubject
//
//    val typesObservable: Observable<List<FilterTransactionType>> = Observable.just(FilterTransactionType.values().toList())
//
//    private var selectedTransactionWallet: TransactionWallet? = null
//    private val selectedWalletSubject = BehaviorSubject.create<Optional<TransactionWallet>>()
//    val selectedWalletObservable: Observable<Optional<TransactionWallet>> = selectedWalletSubject
//
//    private val selectedTransactionTypeSubject = BehaviorSubject.createDefault(FilterTransactionType.All)
//    val selectedTransactionTypeObservable: Observable<FilterTransactionType> = selectedTransactionTypeSubject
//
//    fun setWallets(transactionWallets: List<TransactionWallet>) {
//        walletsSubject.onNext(transactionWallets)
//
//        // Reset selectedWallet if it does not exist in the new wallets list or leave it if it does
//        // When there is a coin added or removed then the selectedWallet should be left
//        // When the whole wallets list is changed (switch account) it should be reset
//        if (!transactionWallets.contains(selectedTransactionWallet)) {
//            selectedTransactionWallet = null
//            selectedWalletSubject.onNext(Optional.ofNullable(selectedTransactionWallet))
//
//            selectedTransactionTypeSubject.onNext(FilterTransactionType.All)
//        }
//    }
//
//    fun setFilterWallet(transactionWallet: TransactionWallet?) {
//        selectedTransactionWallet = transactionWallet
//        selectedWalletSubject.onNext(Optional.ofNullable(selectedTransactionWallet))
//    }
//
//    fun setFilterType(transactionType: FilterTransactionType) {
//        selectedTransactionTypeSubject.onNext(transactionType)
//    }

}
