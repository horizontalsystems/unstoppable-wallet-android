package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TransactionFilterService(
    private val spamManager: SpamManager,
) {
    private var transactionWallets: List<TransactionWallet?> = listOf(null)
    var selectedWallet: TransactionWallet? = null
        private set
    var selectedTransactionType: FilterTransactionType = FilterTransactionType.All
        private set
    var selectedBlockchain: Blockchain? = null
        private set

    private val _resetEnabled = MutableStateFlow(false)
    val resetEnabled = _resetEnabled.asStateFlow()
    val filterHideSuspiciousTx = spamManager.hideSuspiciousTx

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

        emitResetEnabled()
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

        emitResetEnabled()
    }

    fun setSelectedBlockchain(blockchain: Blockchain?) {
        selectedBlockchain = blockchain
        selectedWallet = null

        emitResetEnabled()
    }

    fun setSelectedTransactionType(type: FilterTransactionType) {
        selectedTransactionType = type

        emitResetEnabled()
    }

    fun reset() {
        selectedTransactionType = FilterTransactionType.All
        selectedWallet = null
        selectedBlockchain = null

        emitResetEnabled()
    }

    private fun emitResetEnabled() {
        _resetEnabled.update {
            selectedWallet != null
                || selectedBlockchain != null
                || selectedTransactionType != FilterTransactionType.All
        }
    }

    fun setFilterHideSuspiciousTx(hide: Boolean) {
        spamManager.updateFilterHideSuspiciousTx(hide)
    }

}
