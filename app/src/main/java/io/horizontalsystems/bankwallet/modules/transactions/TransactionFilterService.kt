package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Blockchain

class TransactionFilterService {
    private var transactionWallets: List<TransactionWallet?> = listOf(null)
    var selectedWallet: TransactionWallet? = null
        private set
    var selectedTransactionType: FilterTransactionType = FilterTransactionType.All
    var selectedBlockchain: Blockchain? = null
        private set

    private var blockchains: List<Blockchain?> = listOf(null)

    fun setWallets(wallets: List<Wallet>) {
        transactionWallets = listOf(null) + wallets.sortedBy { it.coin.code }.map {
            TransactionWallet(it.token, it.transactionSource, it.badge)
        }

        // Reset selectedWallet if it does not exist in the new wallets list or leave it if it does
        // When there is a coin added or removed then the selectedWallet should be left
        // When the whole wallets list is changed (switch account) it should be reset
        if (!transactionWallets.contains(selectedWallet)) {
            selectedWallet = null

            selectedTransactionType = FilterTransactionType.All

            selectedBlockchain = null
        }

        blockchains = listOf(null).plus(wallets.map { it.token.blockchain }.distinct())
        if (!blockchains.contains(selectedBlockchain)) {
            selectedBlockchain = null
        }
    }

    fun getTransactionWallets(): List<TransactionWallet?> {
        return transactionWallets
    }

    fun getFilterTypes(): List<FilterTransactionType> {
        return FilterTransactionType.values().toList()
    }

    fun getBlockchains(): List<Blockchain?> {
        return blockchains
    }

    fun setSelectedWallet(wallet: TransactionWallet?) {
        selectedWallet = wallet
        selectedBlockchain = selectedWallet?.source?.blockchain
    }

    fun setSelectedBlockchain(blockchain: Blockchain?) {
        selectedBlockchain = blockchain
        selectedWallet = null
    }
}
