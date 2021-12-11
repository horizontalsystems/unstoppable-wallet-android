package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Wallet

class TransactionFilterService {
    private var transactionWallets: List<TransactionWallet> = listOf()
    var selectedWallet: TransactionWallet? = null
    var selectedTransactionType: FilterTransactionType = FilterTransactionType.All

    fun setWallets(wallets: List<Wallet>) {
        transactionWallets = wallets.sortedBy { it.coin.code }.map {
            TransactionWallet(it.platformCoin, it.transactionSource, it.badge)
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
}
