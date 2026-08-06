package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.adapters.TonTransactionRecord
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.stats.StatSection
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoViewItem
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoViewItem.Status
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionInfoViewItem.Value
import io.horizontalsystems.walletkit.modules.transactionInfo.TransactionViewItemFactoryHelper
import io.horizontalsystems.walletkit.modules.transactions.TransactionStatus
import io.horizontalsystems.walletkit.modules.transactions.TransactionViewItem
import io.horizontalsystems.marketkit.models.BlockchainType

object TonHelper {
    fun getViewItemsForAction(
        action: TonTransactionRecord.Action,
        rates: Map<String, CurrencyValue>,
        blockchainType: BlockchainType,
        hideAmount: Boolean,
        showHistoricalRate: Boolean
    ): List<TransactionInfoViewItem> {

        val itemsForAction = mutableListOf<TransactionInfoViewItem>()

        when (val actionType = action.type) {
            is TonTransactionRecord.Action.Type.Send -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = actionType.to,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        sentToSelf = actionType.sentToSelf,
                        blockchainType = blockchainType,
                        showHistoricalRate = showHistoricalRate
                    )
                )
                actionType.comment?.let {
                    itemsForAction.add(
                        Value(
                            Translator.getString(R.string.TransactionInfo_Memo),
                            it
                        )
                    )
                }
            }

            is TonTransactionRecord.Action.Type.Receive -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = actionType.value,
                        fromAddress = actionType.from,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                        showHistoricalRate = showHistoricalRate
                    )
                )
                actionType.comment?.let {
                    itemsForAction.add(
                        Value(
                            Translator.getString(R.string.TransactionInfo_Memo),
                            it
                        )
                    )
                }
            }

            is TonTransactionRecord.Action.Type.Burn -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = null,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TonTransactionRecord.Action.Type.Mint -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = actionType.value,
                        fromAddress = TransactionViewItemFactoryHelper.zeroAddress,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TonTransactionRecord.Action.Type.Swap -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSwapEventSectionItems(
                        valueIn = actionType.valueIn,
                        valueOut = actionType.valueOut,
                        rates = rates,
                        amount = null,
                        hideAmount = hideAmount,
                        hasRecipient = false
                    )
                )
            }

            is TonTransactionRecord.Action.Type.ContractDeploy -> {
                itemsForAction.add(
                    TransactionInfoViewItem.Transaction(
                        leftValue = Translator.getString(R.string.Transactions_ContractDeploy),
                        rightValue = actionType.interfaces.joinToString(),
                        icon = null,
                    )
                )

            }

            is TonTransactionRecord.Action.Type.ContractCall -> {
                itemsForAction.add(
                    TransactionInfoViewItem.Transaction(
                        leftValue = Translator.getString(R.string.Transactions_ContractCall),
                        rightValue = actionType.operation,
                        icon = TransactionViewItem.Icon.Platform(blockchainType).iconRes,
                    )
                )

                itemsForAction.add(
                    TransactionInfoViewItem.Address(
                        Translator.getString(R.string.TransactionInfo_To),
                        actionType.address,
                        false,
                        blockchainType,
                        StatSection.AddressTo
                    )
                )

                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = null,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TonTransactionRecord.Action.Type.Unsupported -> {
                itemsForAction.add(Value("Action", actionType.type))
            }
        }

        if (action.status == TransactionStatus.Failed) {
            itemsForAction.add(Status(action.status))
        }

        return itemsForAction


    }
}
