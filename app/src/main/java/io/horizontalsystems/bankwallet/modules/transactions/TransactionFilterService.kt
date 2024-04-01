package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.managers.TransactionAdapterManager
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class TransactionFilterService(
    private val marketKitWrapper: MarketKitWrapper,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val spamManager: SpamManager
) {
    private var blockchains: List<Blockchain?> = listOf(null)
    private var selectedBlockchain: Blockchain? = null
    private var filterTokenList: List<FilterToken?> = listOf(null)
    private var selectedToken: FilterToken? = null
    private val transactionTypes = FilterTransactionType.values().toList()
    private var selectedTransactionType: FilterTransactionType = FilterTransactionType.All
    private var contact: Contact? = null
    private var uniqueId = UUID.randomUUID().toString()
    private var hideSuspiciousTx = spamManager.hideSuspiciousTx

    private val _stateFlow = MutableStateFlow(
        State(
            blockchains = blockchains,
            selectedBlockchain = selectedBlockchain,
            filterTokens = filterTokenList,
            selectedToken = selectedToken,
            transactionTypes = transactionTypes,
            selectedTransactionType = selectedTransactionType,
            resetEnabled = resetEnabled(),
            uniqueId = uniqueId,
            contact = contact,
            hideSuspiciousTx = hideSuspiciousTx,
        )
    )
    val stateFlow get() = _stateFlow.asStateFlow()

    private fun emitState() {
        _stateFlow.update {
            State(
                blockchains = blockchains,
                selectedBlockchain = selectedBlockchain,
                filterTokens = filterTokenList,
                selectedToken = selectedToken,
                transactionTypes = transactionTypes,
                selectedTransactionType = selectedTransactionType,
                resetEnabled = resetEnabled(),
                uniqueId = uniqueId,
                contact = contact,
                hideSuspiciousTx = hideSuspiciousTx,
            )
        }
    }

    fun setWallets(wallets: List<Wallet>) {
        uniqueId = UUID.randomUUID().toString()

        val filterTokens = wallets.map {
            FilterToken(it.token, it.transactionSource)
        }

        val additionalFilterTokens = transactionAdapterManager.adaptersMap.map { map ->
            marketKitWrapper.tokens(map.value.additionalTokenQueries).map {
                FilterToken(it, map.key)
            }
        }.flatten()

        val combinedTokenAndSources = filterTokens.plus(additionalFilterTokens)
        val sortedTokenAndSources = combinedTokenAndSources
            .distinctBy { it.token }
            .sortedBy { it.token.coin.code }

        filterTokenList = listOf(null) + sortedTokenAndSources

        // Reset selectedWallet if it does not exist in the new wallets list or leave it if it does
        // When there is a coin added or removed then the selectedWallet should be left
        // When the whole wallets list is changed (switch account) it should be reset
        if (!filterTokenList.contains(selectedToken)) {
            selectedToken = null

            selectedTransactionType = FilterTransactionType.All

            selectedBlockchain = null
        }

        blockchains = listOf(null).plus(wallets.map { it.token.blockchain }.distinct())

        if (!blockchains.contains(selectedBlockchain)) {
            selectedBlockchain = null
        }

        emitState()
    }

    fun setSelectedToken(filterToken: FilterToken?) {
        selectedToken = filterToken
        selectedBlockchain = selectedToken?.source?.blockchain

        refreshContact()

        emitState()
    }

    fun setSelectedBlockchain(blockchain: Blockchain?) {
        selectedBlockchain = blockchain
        selectedToken = null

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
        selectedToken = null
        selectedBlockchain = null
        contact = null
        hideSuspiciousTx = true
        spamManager.updateFilterHideSuspiciousTx(true)

        emitState()
    }

    private fun resetEnabled(): Boolean {
        return selectedToken != null
                || selectedBlockchain != null
                || contact != null
                || !hideSuspiciousTx
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

    fun updateFilterHideSuspiciousTx(checked: Boolean) {
        hideSuspiciousTx = checked
        spamManager.updateFilterHideSuspiciousTx(checked)
        emitState()
    }

    data class State(
        val blockchains: List<Blockchain?>,
        val selectedBlockchain: Blockchain?,
        val filterTokens: List<FilterToken?>,
        val selectedToken: FilterToken?,
        val transactionTypes: List<FilterTransactionType>,
        val selectedTransactionType: FilterTransactionType,
        val resetEnabled: Boolean,
        val uniqueId: String,
        val contact: Contact?,
        val hideSuspiciousTx: Boolean,
    )

}
