package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValueNew
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import java.util.*

class TransactionViewItemFactory {

    private val cache = mutableMapOf<String, Map<Long, TransactionViewItem>>()

    fun convertToViewItemCached(transactionItem: TransactionItem): TransactionViewItem {
        cache.get(transactionItem.record.uid)?.get(transactionItem.createdAt)?.let {
            return it
        }

        val transactionViewItem = convertToViewItem(transactionItem)
        cache[transactionItem.record.uid] = mapOf(transactionItem.createdAt to transactionViewItem)

        return transactionViewItem
    }

    private fun convertToViewItem(transactionItem: TransactionItem): TransactionViewItem {
        val record = transactionItem.record
        val status = record.status(transactionItem.lastBlockInfo?.height)
        val progress = when (status) {
            is TransactionStatus.Pending -> 15
            is TransactionStatus.Processing -> (status.progress * 100).toInt()
            else -> null
        }
        val icon = if (status is TransactionStatus.Failed) R.drawable.ic_attention_red_20 else null

        val lastBlockTimestamp = transactionItem.lastBlockInfo?.timestamp

        return when (record) {
            is ApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is BinanceChainIncomingTransactionRecord -> createViewItemFromBinanceChainIncomingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is BinanceChainOutgoingTransactionRecord -> createViewItemFromBinanceChainOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is BitcoinIncomingTransactionRecord -> createViewItemFromBitcoinIncomingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is BitcoinOutgoingTransactionRecord -> createViewItemFromBitcoinOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is ContractCallTransactionRecord -> createViewItemFromContractCallTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is ExternalContractCallTransactionRecord -> createViewItemFromExternalContractCallTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is EvmIncomingTransactionRecord -> createViewItemFromEvmIncomingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is EvmOutgoingTransactionRecord -> createViewItemFromEvmOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is UnknownSwapTransactionRecord -> createViewItemFromUnknownSwapTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is EvmTransactionRecord -> createViewItemFromEvmTransactionRecord(record, progress, icon)
            else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
        }
    }

    private fun createViewItemFromSwapTransactionRecord(
        record: SwapTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = ColoredValueNew(getCoinString(record.valueIn), ColorName.Jacob)
        val secondaryValue = record.valueOut?.let {
            ColoredValueNew(
                getCoinString(it),
                if (record.recipient != null) ColorName.Grey else ColorName.Remus
            )
        }

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_swap_20,
            progress,
            Translator.getString(R.string.Transactions_Swap),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.exchangeAddress)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromUnknownSwapTransactionRecord(
        record: UnknownSwapTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = record.valueIn?.let { ColoredValueNew(getCoinString(it), ColorName.Jacob) }
        val secondaryValue = record.valueOut?.let { ColoredValueNew(getCoinString(it), ColorName.Remus) }

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_swap_20,
            progress,
            Translator.getString(R.string.Transactions_Swap),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.exchangeAddress)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromEvmTransactionRecord(record: EvmTransactionRecord, progress: Int?, icon: Int?): TransactionViewItem {
        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_swap_20,
            progress,
            Translator.getString(R.string.Transactions_Unknown),
            Translator.getString(R.string.Transactions_Unknown_Description),
            null,
            null,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        record: EvmOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Jacob)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf
        )
    }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        record: EvmIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Remus)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromContractCreationTransactionRecord(
        record: ContractCreationTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_unordered,
            progress,
            Translator.getString(R.string.Transactions_ContractCreation),
            "---",
            null,
            null,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromContractCallTransactionRecord(
        record: ContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val incomingEvents = combinedEvents(record.incomingEvents)
        val outgoingEvents = combinedEvents(record.outgoingEvents)

        val (primaryValue: ColoredValueNew?, secondaryValue: ColoredValueNew?) = getValuesFromEvents(incomingEvents, outgoingEvents, currencyValue)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_unordered,
            progress,
            record.blockchainTitle + " " + Translator.getString(R.string.Transactions_ContractCall),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.contractAddress)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromExternalContractCallTransactionRecord(
        record: ExternalContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val incomingEvents = combinedEvents(record.incomingEvents)
        val outgoingEvents = combinedEvents(record.outgoingEvents)

        val (primaryValue: ColoredValueNew?, secondaryValue: ColoredValueNew?) = getValuesFromEvents(incomingEvents, outgoingEvents, currencyValue)

        val transactionTypeIcon = if (outgoingEvents.isNotEmpty()) R.drawable.ic_tx_unordered else R.drawable.ic_incoming_20
        val title: String
        val subTitle: String
        if (outgoingEvents.isEmpty()) {
            title = Translator.getString(R.string.Transactions_Receive)
            val addresses = record.incomingEvents.mapNotNull { it.address }.toSet().toList()

            subTitle = if (addresses.size == 1) Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(addresses.first())
            ) else Translator.getString(R.string.Transactions_Multiple)
        } else {
            title = record.blockchainTitle + " " + Translator.getString(R.string.Transactions_ExternalContractCall)
            subTitle = "---"
        }

        return TransactionViewItem(
            record.uid,
            icon ?: transactionTypeIcon,
            progress,
            title,
            subTitle,
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromBitcoinOutgoingTransactionRecord(
        record: BitcoinOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val subtitle = record.to?.let {
            Translator.getString(
                R.string.Transactions_To,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Jacob)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            subtitle,
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf,
            record.conflictingHash != null,
            locked
        )
    }

    private fun createViewItemFromBitcoinIncomingTransactionRecord(
        record: BitcoinIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val subtitle = record.from?.let {
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Remus)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            subtitle,
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            false,
            record.conflictingHash != null,
            locked
        )
    }

    private fun createViewItemFromBinanceChainOutgoingTransactionRecord(
        record: BinanceChainOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Jacob)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_outgoing_20,
            progress,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000),
            record.sentToSelf
        )
    }

    private fun createViewItemFromBinanceChainIncomingTransactionRecord(
        record: BinanceChainIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValueNew(getCurrencyString(it), ColorName.Remus)
        }
        val secondaryValue = ColoredValueNew(getCoinString(record.value), ColorName.Grey)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_incoming_20,
            progress,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        record: ApproveTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValueText: String?
        val secondaryValueText: String

        if (record.value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText = if (record.value.coinCode.isEmpty()) "" else Translator.getString(R.string.Transaction_Unlimited, record.value.coinCode)
        } else {
            primaryValueText = currencyValue?.let { getCurrencyString(it) }
            secondaryValueText = getCoinString(record.value)
        }

        val primaryValue = primaryValueText?.let { ColoredValueNew(it, ColorName.Leah) }
        val secondaryValue = ColoredValueNew(secondaryValueText, ColorName.Grey)

        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_checkmark_20,
            progress,
            Translator.getString(R.string.Transactions_Approve),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.spender)
            ),
            primaryValue,
            secondaryValue,
            Date(record.timestamp * 1000)
        )
    }

    private fun combinedEvent(event1: EvmTransactionRecord.TransferEvent, event2: EvmTransactionRecord.TransferEvent): EvmTransactionRecord.TransferEvent? {
        val value1 = event1.value
        val value2 = event2.value

        if (value1 is TransactionValue.CoinValue && value2 is TransactionValue.CoinValue) {
            if (value2 is TransactionValue.CoinValue && value1.platformCoin == value2.platformCoin) {
                return EvmTransactionRecord.TransferEvent(
                    event1.address,
                    TransactionValue.CoinValue(value1.platformCoin, value1.value + value2.value)
                )
            }
        }
        if (value1 is TransactionValue.TokenValue && value2 is TransactionValue.TokenValue) {
            if (value2 is TransactionValue.TokenValue &&
                value1.tokenName == value2.tokenName &&
                value1.tokenCode == value2.tokenCode &&
                value1.tokenDecimals == value2.tokenDecimals) {
                return EvmTransactionRecord.TransferEvent(
                    event1.address,
                    TransactionValue.TokenValue("", value1.tokenName, value1.tokenCode, value1.tokenDecimals, value1.value + value2.value)
                )
            }
        }

        return null
    }

    private fun combinedEvents(events: List<EvmTransactionRecord.TransferEvent>): List<EvmTransactionRecord.TransferEvent> {
        val results: MutableList<EvmTransactionRecord.TransferEvent> = mutableListOf()

        events.forEachIndexed { index, event1 ->
            if (!results.any { combinedEvent(event1, it) != null }) {
                var sum = event1

                events.subList(index + 1, events.size).forEach { event2 ->
                    sum = combinedEvent(sum, event2) ?: sum
                }

                results.add(sum)
            }
        }

        return results
    }

    private fun getValuesFromEvents(incomingEvents: List<EvmTransactionRecord.TransferEvent>, outgoingEvents: List<EvmTransactionRecord.TransferEvent>, currencyValue: CurrencyValue?): Pair<ColoredValueNew?, ColoredValueNew?> {
        val primaryValue: ColoredValueNew?
        val secondaryValue: ColoredValueNew?

        val incomingValues = incomingEvents.map { it.value }
        val outgoingValues = outgoingEvents.map { it.value }

        when {
            (incomingValues.size == 1 && outgoingValues.isEmpty()) -> {
                primaryValue = currencyValue?.let {
                    ColoredValueNew(getCurrencyString(it), ColorName.Remus)
                }
                secondaryValue = ColoredValueNew(getCoinString(incomingValues.first()), ColorName.Grey)
            }

            (incomingValues.isEmpty() && outgoingValues.size == 1) -> {
                primaryValue = currencyValue?.let {
                    ColoredValueNew(getCurrencyString(it), ColorName.Jacob)
                }
                secondaryValue = ColoredValueNew(getCoinString(outgoingValues.first()), ColorName.Grey)
            }

            (incomingValues.size == 1 && outgoingValues.size == 1) -> {
                secondaryValue = ColoredValueNew(getCoinString(outgoingValues.first()), ColorName.Jacob)
                primaryValue = ColoredValueNew(getCoinString(incomingValues.first()), ColorName.Remus)
            }

            (incomingValues.isEmpty() && outgoingValues.isNotEmpty()) -> {
                primaryValue = ColoredValueNew(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Jacob)
                secondaryValue = ColoredValueNew(Translator.getString(R.string.Transactions_Multiple), ColorName.Grey)
            }

            (incomingValues.isNotEmpty() && outgoingValues.isEmpty()) -> {
                primaryValue = ColoredValueNew(incomingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Remus)
                secondaryValue = ColoredValueNew(Translator.getString(R.string.Transactions_Multiple), ColorName.Grey)
            }

            else -> {
                primaryValue = ColoredValueNew(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Jacob)
                secondaryValue = ColoredValueNew(incomingValues.joinToString(", ") { it.coinCode }, ColorName.Remus)
            }
        }
        return Pair(primaryValue, secondaryValue)
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiat(currencyValue.value.abs(), currencyValue.currency.symbol, 0, 2)
    }

    private fun getCoinString(transactionValue: TransactionValue): String {
        return transactionValue.decimalValue?.let { decimalValue ->
            val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(decimalValue)
            App.numberFormatter.formatCoin(
                decimalValue.abs(),
                transactionValue.coinCode,
                0,
                significantDecimal
            )
        } ?: ""
    }

    private fun getNameOrAddressTruncated(address: String): String {
        return TransactionInfoAddressMapper.title(address) ?: "${address.take(5)}...${address.takeLast(5)}"
    }
}