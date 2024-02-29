package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class TransactionFilterService {
    private var blockchains: List<Blockchain?> = listOf(null)
    private var selectedBlockchain: Blockchain? = null
    private var transactionWallets: List<TransactionWallet?> = listOf(null)
    private var selectedWallet: TransactionWallet? = null
    private val transactionTypes = FilterTransactionType.values().toList()
    private var selectedTransactionType: FilterTransactionType = FilterTransactionType.All
    private var contact: Contact? = null
    private var uniqueId = UUID.randomUUID().toString()

    private val _stateFlow = MutableStateFlow(
        State(
            blockchains = blockchains,
            selectedBlockchain = selectedBlockchain,
            transactionWallets = transactionWallets,
            selectedWallet = selectedWallet,
            transactionTypes = transactionTypes,
            selectedTransactionType = selectedTransactionType,
            resetEnabled = resetEnabled(),
            uniqueId = uniqueId,
            contact = contact
        )
    )
    val stateFlow get() = _stateFlow.asStateFlow()

    private fun emitState() {
        _stateFlow.update {
            State(
                blockchains = blockchains,
                selectedBlockchain = selectedBlockchain,
                transactionWallets = transactionWallets,
                selectedWallet = selectedWallet,
                transactionTypes = transactionTypes,
                selectedTransactionType = selectedTransactionType,
                resetEnabled = resetEnabled(),
                uniqueId = uniqueId,
                contact = contact
            )
        }
    }

    fun setWallets(wallets: List<Wallet>) {
        uniqueId = UUID.randomUUID().toString()

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

        emitState()
    }

    fun setSelectedWallet(wallet: TransactionWallet?) {
        selectedWallet = wallet
        selectedBlockchain = selectedWallet?.source?.blockchain

        refreshContact()

        emitState()
    }

    fun setSelectedBlockchain(blockchain: Blockchain?) {
        selectedBlockchain = blockchain
        selectedWallet = null

        refreshContact()

        emitState()
    }

    fun setSelectedTransactionType(type: FilterTransactionType) {
        selectedTransactionType = type

        emitState()
    }

    fun setContact(contact: Contact?) {
        this.contact = contact

        emitState()
    }

    fun reset() {
        selectedTransactionType = FilterTransactionType.All
        selectedWallet = null
        selectedBlockchain = null
        contact = null

        emitState()
    }

    private fun resetEnabled(): Boolean {
        return selectedWallet != null
            || selectedBlockchain != null
            || selectedTransactionType != FilterTransactionType.All
            || contact != null
    }

    private fun refreshContact() {
        val tmpBlockchain = selectedBlockchain ?: return
        val tmpContact = contact ?: return

        if (!SelectContactViewModel.supportedBlockchainTypes.contains(tmpBlockchain.type)) {
            contact = null
        } else if (tmpContact.addresses.none { it.blockchain.type == tmpBlockchain.type }) {
            contact = null
        }
    }

    data class State(
        val blockchains: List<Blockchain?>,
        val selectedBlockchain: Blockchain?,
        val transactionWallets: List<TransactionWallet?>,
        val selectedWallet: TransactionWallet?,
        val transactionTypes: List<FilterTransactionType>,
        val selectedTransactionType: FilterTransactionType,
        val resetEnabled: Boolean,
        val uniqueId: String,
        val contact: Contact?,
    )

}
