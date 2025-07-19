package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.R
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.core.managers.TonHelper
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.monero.MoneroTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaTransactionRecord
import cash.p.terminal.entities.transactionrecords.stellar.StellarTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.SentToSelf
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.SpeedUpCancel
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.Transaction
import cash.p.terminal.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.modules.transactions.TransactionViewItem
import cash.p.terminal.strings.helpers.Translator
import io.horizontalsystems.core.entities.BlockchainType

class TransactionInfoViewItemFactory(
    private val resendEnabled: Boolean,
    private val blockchainType: BlockchainType,
) {
    fun getViewItemSections(transactionItem: TransactionInfoItem): List<List<TransactionInfoViewItem>> {
        val transaction = transactionItem.record
        val rates = transactionItem.rates
        val nftMetadata = transactionItem.nftMetadata
        val status = transactionItem.externalStatus
            ?: transaction.status(transactionItem.lastBlockInfo?.height)
        val itemSections = mutableListOf<List<TransactionInfoViewItem>>()
        val miscItemsSection = mutableListOf<TransactionInfoViewItem>()

        var sentToSelf = false

        if (transactionItem.record.spam) {
            itemSections.add(
                listOf(
                    TransactionInfoViewItem.WarningMessage(
                        Translator.getString(
                            R.string.TransactionInfo_SpamWarning
                        )
                    )
                )
            )
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
                                    TransactionInfoViewItem.Value(
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
                                    TransactionInfoViewItem.Value(
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
                                TransactionInfoViewItem.Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    Translator.getString(R.string.Transactions_OperationType_ChangeTrust)
                                )
                            )
                        )
                    }

                    is StellarTransactionRecord.Type.Unsupported -> {
                        itemSections.add(
                            listOf(
                                TransactionInfoViewItem.Value(
                                    Translator.getString(R.string.Transactions_OperationType),
                                    transactionType.type
                                )
                            )
                        )
                    }
                }

                addMemoItem(transaction.memo, miscItemsSection)
            }

            is EvmTransactionRecord -> {
                when (transaction.transactionRecordType) {
                    TransactionRecordType.EVM_CONTRACT_CREATION -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getContractCreationItems(
                                transaction
                            )
                        )
                    }

                    TransactionRecordType.EVM_INCOMING -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transaction.value!!,
                                fromAddress = transaction.from,
                                coinPrice = rates[transaction.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.EVM_OUTGOING -> {
                        sentToSelf = transaction.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transaction.value!!,
                                toAddress = transaction.to,
                                coinPrice = rates[transaction.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transaction.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.EVM_SWAP -> {
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
                                rates = rates,
                                exchangeAddress = transaction.exchangeAddress!!,
                                valueOut = transaction.valueOut,
                                valueIn = transaction.valueIn
                            )
                        )
                    }

                    TransactionRecordType.EVM_UNKNOWN_SWAP -> {
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
                                rates = rates,
                                exchangeAddress = transaction.exchangeAddress!!,
                                valueOut = transaction.valueOut,
                                valueIn = transaction.valueIn,
                            )
                        )
                    }

                    TransactionRecordType.EVM_APPROVE -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getApproveSectionItems(
                                value = transaction.value!!,
                                coinPrice = rates[transaction.value.coinUid],
                                spenderAddress = transaction.spender!!,
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.EVM_CONTRACT_CALL -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                                transaction.method,
                                transaction.contractAddress!!,
                                transaction.blockchainType
                            )
                        )

                        for (event in transaction.outgoingEvents!!) {
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

                        for (event in transaction.incomingEvents!!) {
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

                    TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL -> {
                        for (event in transaction.outgoingEvents!!) {
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

                        for (event in transaction.incomingEvents!!) {
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

                    else -> {}
                }
            }

            is TonTransactionRecord -> {
                transaction.actions.forEach { action ->
                    itemSections.add(
                        TonHelper.getViewItemsForAction(
                            action,
                            rates,
                            blockchainType,
                            transactionItem.hideAmount
                        )
                    )
                }

//            feeViewItem = record.fee.map { .fee(title: "tx_info.fee".localized, value: feeString(transactionValue: $0, rate: _rate($0))) }
            }

            is TronTransactionRecord -> {
                when (transaction.transactionRecordType) {
                    TransactionRecordType.TRON_INCOMING ->
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transaction.value!!,
                                fromAddress = transaction.from,
                                coinPrice = rates[transaction.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )

                    TransactionRecordType.TRON_OUTGOING -> {
                        sentToSelf = transaction.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transaction.value!!,
                                toAddress = transaction.to,
                                coinPrice = rates[transaction.value.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transaction.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.TRON_APPROVE ->
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getApproveSectionItems(
                                value = transaction.value!!,
                                coinPrice = rates[transaction.value.coinUid],
                                spenderAddress = transaction.spender!!,
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )

                    TransactionRecordType.TRON_CONTRACT_CALL -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getContractMethodSectionItems(
                                transaction.method,
                                transaction.contractAddress!!,
                                transaction.blockchainType
                            )
                        )

                        for (event in transaction.outgoingEvents!!) {
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

                        for (event in transaction.incomingEvents!!) {
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

                    TransactionRecordType.TRON_EXTERNAL_CONTRACT_CALL -> {
                        for (event in transaction.outgoingEvents!!) {
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

                        for (event in transaction.incomingEvents!!) {
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

                    TransactionRecordType.TRON ->
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

                    else -> {}
                }
            }

            is BitcoinTransactionRecord -> {
                when (transaction.transactionRecordType) {
                    TransactionRecordType.BITCOIN_INCOMING -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transaction.mainValue,
                                fromAddress = transaction.from,
                                coinPrice = rates[transaction.mainValue.coinUid],
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

                    TransactionRecordType.BITCOIN_OUTGOING -> {
                        sentToSelf = transaction.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transaction.mainValue,
                                toAddress = transaction.to,
                                coinPrice = rates[transaction.mainValue.coinUid],
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

                    else -> {}

                }
            }

            is SolanaTransactionRecord -> {
                when (transaction.transactionRecordType) {
                    TransactionRecordType.SOLANA_INCOMING -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transaction.mainValue!!,
                                fromAddress = transaction.from,
                                coinPrice = rates[transaction.mainValue!!.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.SOLANA_OUTGOING -> {
                        sentToSelf = transaction.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transaction.mainValue!!,
                                toAddress = transaction.to,
                                coinPrice = rates[transaction.mainValue!!.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transaction.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.SOLANA_UNKNOWN -> {
                        for (transfer in transaction.outgoingSolanaTransfers!!) {
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

                        for (transfer in transaction.incomingSolanaTransfers!!) {
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
            }

            is MoneroTransactionRecord -> {
                when (transaction.transactionRecordType) {
                    TransactionRecordType.MONERO_INCOMING -> {
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getReceiveSectionItems(
                                value = transaction.mainValue,
                                fromAddress = transaction.from,
                                coinPrice = rates[transaction.mainValue.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    TransactionRecordType.MONERO_OUTGOING -> {
                        sentToSelf = transaction.sentToSelf
                        itemSections.add(
                            TransactionViewItemFactoryHelper.getSendSectionItems(
                                value = transaction.mainValue,
                                toAddress = transaction.to,
                                coinPrice = rates[transaction.mainValue.coinUid],
                                hideAmount = transactionItem.hideAmount,
                                sentToSelf = transaction.sentToSelf,
                                nftMetadata = nftMetadata,
                                blockchainType = blockchainType,
                            )
                        )
                    }

                    else -> Unit
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

        itemSections.add(
            TransactionViewItemFactoryHelper.getStatusSectionItems(
                transaction = transaction,
                status = status,
                rates = rates,
                blockchainType = blockchainType
            )
        )
        if (transaction is EvmTransactionRecord && !transaction.foreignTransaction && status == TransactionStatus.Pending && resendEnabled) {
            itemSections.add(
                listOf(
                    SpeedUpCancel(
                        transactionHash = transaction.transactionHash,
                        blockchainType = transaction.blockchainType
                    )
                )
            )
            itemSections.add(
                listOf(
                    TransactionInfoViewItem.Description(
                        Translator.getString(
                            R.string.TransactionInfo_SpeedUpDescription
                        )
                    )
                )
            )
        } else if (transaction is BitcoinTransactionRecord &&
            transaction.transactionRecordType == TransactionRecordType.BITCOIN_OUTGOING &&
            transaction.replaceable && resendEnabled
        ) {
            itemSections.add(
                listOf(
                    SpeedUpCancel(
                        transactionHash = transaction.transactionHash,
                        blockchainType = transaction.blockchainType
                    )
                )
            )
            itemSections.add(
                listOf(
                    TransactionInfoViewItem.Description(
                        Translator.getString(
                            R.string.TransactionInfo_SpeedUpDescription
                        )
                    )
                )
            )
        }
        itemSections.add(TransactionViewItemFactoryHelper.getExplorerSectionItems(transactionItem.explorerData))
        transactionItem.transactionStatusUrl?.let {
            itemSections.add(
                TransactionViewItemFactoryHelper.getExplorerSectionItems(
                    TransactionInfoModule.ExplorerData(
                        title = it.first,
                        url = it.second
                    )
                )
            )
        }

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
