package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.managers.TonHelper
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SentToSelf
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SpeedUpCancel
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Transaction
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

class TransactionInfoViewItemFactory(
    private val resendEnabled: Boolean,
    private val blockchainType: BlockchainType,
) {
    fun getViewItemSections(transactionItem: TransactionInfoItem): List<List<TransactionInfoViewItem>> {
        val transaction = transactionItem.record
        val rates = transactionItem.rates
        val nftMetadata = transactionItem.nftMetadata

        val status = transaction.status(transactionItem.lastBlockInfo?.height)
        val itemSections = mutableListOf<List<TransactionInfoViewItem>>()
        val miscItemsSection = mutableListOf<TransactionInfoViewItem>()

        var sentToSelf = false

        if (transactionItem.record.spam) {
            itemSections.add(listOf(TransactionInfoViewItem.WarningMessage(Translator.getString(R.string.TransactionInfo_SpamWarning))))
        }

        when (transaction) {
            is ContractCreationTransactionRecord -> {
                itemSections.add(TransactionViewItemFactoryHelper.getContractCreationItems(transaction))
            }

            is TonTransactionRecord -> {
                transaction.actions.forEach { action ->
                    itemSections.add(
                        TonHelper.getViewItemsForAction(
                            action,
                            rates,
                            blockchainType,
                            transactionItem.hideAmount,
                            true
                        )
                    )
                }

//            feeViewItem = record.fee.map { .fee(title: "tx_info.fee".localized, value: feeString(transactionValue: $0, rate: _rate($0))) }
            }
            is EvmIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is TronIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is EvmOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TronOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is SwapTransactionRecord -> {
                itemSections.add(
                    getSwapEventSectionItems(
                        valueIn = transaction.valueIn,
                        valueOut = transaction.valueOut,
                        rates = rates,
                        amount = transaction.amountIn,
                        hideAmount = transactionItem.hideAmount,
                        hasRecipient = transaction.recipient != null
                    )
                )

                itemSections.add(
                    TransactionViewItemFactoryHelper.getSwapDetailsSectionItems(
                        rates,
                        transaction.exchangeAddress,
                        transaction.valueOut,
                        transaction.valueIn
                    )
                )
            }

            is UnknownSwapTransactionRecord -> {
                itemSections.add(
                    getSwapEventSectionItems(
                        valueIn = transaction.valueIn,
                        valueOut = transaction.valueOut,
                        amount = null,
                        rates = rates,
                        hideAmount = transactionItem.hideAmount,
                        hasRecipient = false
                    )
                )

                itemSections.add(
                    TransactionViewItemFactoryHelper.getSwapDetailsSectionItems(
                        rates,
                        transaction.exchangeAddress,
                        transaction.valueOut,
                        transaction.valueIn,
                    )
                )
            }

            is ApproveTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getApproveSectionItems(
                        value = transaction.value,
                        coinPrice = rates[transaction.value.coinUid],
                        spenderAddress = transaction.spender,
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is TronApproveTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getApproveSectionItems(
                        value = transaction.value,
                        coinPrice = rates[transaction.value.coinUid],
                        spenderAddress = transaction.spender,
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

            is ContractCallTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                        transaction.method,
                        transaction.contractAddress,
                        transaction.blockchainType
                    )
                )

                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronContractCallTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                        transaction.method,
                        transaction.contractAddress,
                        transaction.blockchainType
                    )
                )

                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is ExternalContractCallTransactionRecord -> {
                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronExternalContractCallTransactionRecord -> {
                for (event in transaction.outgoingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = event.value,
                            toAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = event.value,
                            fromAddress = event.address,
                            coinPrice = rates[event.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            is TronTransactionRecord -> {
                itemSections.add(
                    listOf(
                        Transaction(
                            transaction.transaction.contract?.label
                                ?: Translator.getString(R.string.Transactions_ContractCall),
                            "",
                            TransactionViewItem.Icon.Platform(transaction.blockchainType).iconRes
                        )
                    )
                )
            }

            is BitcoinIncomingTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                    )
                )

                miscItemsSection.addAll(
                    TransactionViewItemFactoryHelper.getBitcoinSectionItems(
                        transaction,
                        transactionItem.lastBlockInfo
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is BitcoinOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        blockchainType = blockchainType,
                    )
                )

                miscItemsSection.addAll(
                    TransactionViewItemFactoryHelper.getBitcoinSectionItems(
                        transaction,
                        transactionItem.lastBlockInfo
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is SolanaIncomingTransactionRecord ->
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )

            is SolanaOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = transaction.to,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = transaction.sentToSelf,
                        nftMetadata = nftMetadata,
                        blockchainType = blockchainType,
                    )
                )
            }

            is SolanaUnknownTransactionRecord -> {
                for (transfer in transaction.outgoingTransfers) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getSendSectionItems(
                            value = transfer.value,
                            toAddress = transfer.address,
                            coinPrice = rates[transfer.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }

                for (transfer in transaction.incomingTransfers) {
                    itemSections.add(
                        TransactionViewItemFactoryHelper.getReceiveSectionItems(
                            value = transfer.value,
                            fromAddress = transfer.address,
                            coinPrice = rates[transfer.value.coinUid],
                            hideAmount = transactionItem.hideAmount,
                            nftMetadata = nftMetadata,
                            blockchainType = blockchainType,
                        )
                    )
                }
            }

            else -> {}
        }

        if (sentToSelf) {
            miscItemsSection.add(SentToSelf)
        }
        if (miscItemsSection.isNotEmpty()) {
            itemSections.add(miscItemsSection)
        }

        itemSections.add(TransactionViewItemFactoryHelper.getStatusSectionItems(transaction, status, rates, blockchainType))
        if (transaction is EvmTransactionRecord && !transaction.foreignTransaction && status == TransactionStatus.Pending && resendEnabled) {
            itemSections.add(
                listOf(
                    SpeedUpCancel(
                        transactionHash = transaction.transactionHash,
                        blockchainType = transaction.blockchainType
                    )
                )
            )
            itemSections.add(listOf(TransactionInfoViewItem.Description(Translator.getString(R.string.TransactionInfo_SpeedUpDescription))))
        } else if (transaction is BitcoinOutgoingTransactionRecord && transaction.replaceable && resendEnabled) {
            itemSections.add(
                listOf(
                    SpeedUpCancel(
                        transactionHash = transaction.transactionHash,
                        blockchainType = transaction.blockchainType
                    )
                )
            )
            itemSections.add(listOf(TransactionInfoViewItem.Description(Translator.getString(R.string.TransactionInfo_SpeedUpDescription))))
        }
        itemSections.add(TransactionViewItemFactoryHelper.getExplorerSectionItems(transactionItem.explorerData))

        return itemSections
    }

    private fun addMemoItem(
        memo: String?,
        miscItemsSection: MutableList<TransactionInfoViewItem>,
    ) {
        if (!memo.isNullOrBlank()) {
            miscItemsSection.add(
                TransactionViewItemFactoryHelper.getMemoItem(memo)
            )
        }
    }
}
