package com.quantum.wallet.bankwallet.modules.transactionInfo

import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.adapters.StellarTransactionRecord
import com.quantum.wallet.bankwallet.core.adapters.TonTransactionRecord
import com.quantum.wallet.bankwallet.core.managers.TonHelper
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.monero.MoneroIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.zcash.ZcashShieldingTransactionRecord
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SentToSelf
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionInfoViewItem.SpeedUpCancel
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Transaction
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Value
import com.quantum.wallet.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import com.quantum.wallet.bankwallet.modules.transactions.TransactionStatus
import com.quantum.wallet.bankwallet.modules.transactions.TransactionViewItem
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
            is StellarTransactionRecord -> {
                when (val transactionType = transaction.type) {
                    is StellarTransactionRecord.Type.Receive -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transactionType.value,
                                fromAddress = transactionType.from,
                                coinPrice = rates[transactionType.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )

                        if (transactionType.accountCreated) {
                            itemSections.add(
                                listOf(
                                    Value(
                                        Translator.getString(R.string.Transactions_OperationType),
                                        Translator.getString(R.string.Transactions_OperationType_CreateAccount)
                                    )
                                )
                            )
                        }
                    }

                    is StellarTransactionRecord.Type.Send -> {
                        sentToSelf = transactionType.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transactionType.value,
                                toAddress = transactionType.to,
                                coinPrice = rates[transactionType.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transactionType.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )

                        if (transactionType.accountCreated) {
                            itemSections.add(
                                listOf(
                                    Value(
                                        Translator.getString(R.string.Transactions_OperationType),
                                        Translator.getString(R.string.Transactions_OperationType_CreateAccount)
                                    )
                                )
                            )
                        }
                    }

                    is StellarTransactionRecord.Type.ChangeTrust -> {
                        itemSections.add(
                            listOf(
                                Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    Translator.getString(R.string.Transactions_OperationType_ChangeTrust)
                                )
                            )
                        )
                    }

                    is StellarTransactionRecord.Type.Unsupported -> {
                        itemSections.add(
                            listOf(
                                Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    transactionType.type
                                )
                            )
                        )
                    }
                }

                addMemoItem(transaction.memo, miscItemsSection)
            }

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
                        toAddress = transaction.to
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

            is MoneroIncomingTransactionRecord -> {
                itemSections.add(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = transaction.value,
                        fromAddress = transaction.from,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        blockchainType = blockchainType,
                        toAddress = transaction.to
                    )
                )
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is MoneroOutgoingTransactionRecord -> {
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
                addMemoItem(transaction.memo, miscItemsSection)
                if (!transaction.txKey.isNullOrEmpty()) {
                    miscItemsSection.add(TransactionInfoViewItem.TransactionSecretKey(transaction.txKey))
                }
            }

            is ZcashShieldingTransactionRecord -> {
                itemSections.add(
                    listOf(
                        Transaction(
                            Translator.getString(transaction.direction.title),
                            "",
                            transaction.direction.icon
                        )
                    )
                )
                itemSections.add(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = transaction.value,
                        toAddress = null,
                        coinPrice = rates[transaction.value.coinUid],
                        hideAmount = transactionItem.hideAmount,
                        sentToSelf = true,
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

        if (resendEnabled) {
            when (transaction) {
                is EvmTransactionRecord -> {
                    if (!transaction.foreignTransaction && status == TransactionStatus.Pending && !transaction.protected) {
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
                }

                is BitcoinOutgoingTransactionRecord -> {
                    if (transaction.replaceable) {
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
                }
            }
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
