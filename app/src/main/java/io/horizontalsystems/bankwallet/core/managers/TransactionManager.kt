package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.reactivex.disposables.CompositeDisposable

class TransactionManager(
        private val storage: ITransactionRecordStorage,
        private val rateSyncer: ITransactionRateSyncer,
        private val walletManager: IWalletManager,
        private val currencyManager: ICurrencyManager,
        wordsManager: IWordsManager,
        networkAvailabilityManager: NetworkAvailabilityManager) {

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var adapterDisposables: CompositeDisposable = CompositeDisposable()

    init {
        resubscribeToAdapters()

        disposables.add(walletManager.walletsSubject
                .subscribe {
                    resubscribeToAdapters()
                })

        disposables.add(currencyManager.subject
                .subscribe{
                    handleCurrencyChange()
                })

        disposables.add(wordsManager.loggedInSubject
                .subscribe{ logInState ->
                    if (logInState == LogInState.LOGOUT ) {
                        clear()
                    }
                })

        disposables.add(networkAvailabilityManager.stateSubject
                .subscribe { connected ->
                    if (connected) {
                        syncRates()
                    }
                })
    }

    private fun resubscribeToAdapters() {
        adapterDisposables.clear()

        walletManager.wallets.forEach { wallet ->
            adapterDisposables.add(wallet.adapter.transactionRecordsSubject
                    .subscribe { records ->
                        handle(records, wallet.coin)
                    })
        }
    }

    private fun handle(records: List<TransactionRecord>, coin: String) {
        records.forEach { record ->
            record.coin = coin
        }

        storage.update(records)
        syncRates()
    }

    private fun handleCurrencyChange() {
        storage.clearRates()
        syncRates()
    }

    private fun syncRates() {
        rateSyncer.sync(currencyManager.baseCurrency.code)
    }

    private fun clear() {
        storage.deleteAll()
    }

}
