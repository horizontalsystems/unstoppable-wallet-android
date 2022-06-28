package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory(
    private val evmLabelManager: EvmLabelManager
) {

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
            is TransactionStatus.Pending -> 0.15f
            is TransactionStatus.Processing -> status.progress
            else -> null
        }
        val icon = if (status is TransactionStatus.Failed) TransactionViewItem.Icon.Failed else null

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
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = record.valueOut?.let {
            getColoredValue(it, if (record.recipient != null) ColorName.Grey else ColorName.Remus)
        }
        val secondaryValue = getColoredValue(record.valueIn, ColorName.Lucian)

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Swap),
            subtitle = evmLabelManager.mapped(record.exchangeAddress),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Swap(
                iconIn = TransactionViewItem.Icon.Regular(record.valueIn.coinIconUrl, record.valueIn.coinIconPlaceholder),
                iconOut = TransactionViewItem.Icon.Regular(record.valueOut?.coinIconUrl, record.valueOut?.coinIconPlaceholder),
            )
        )
    }

    private fun createViewItemFromUnknownSwapTransactionRecord(
        record: UnknownSwapTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = record.valueOut?.let { getColoredValue(it, ColorName.Remus) }
        val secondaryValue = record.valueIn?.let { getColoredValue(it, ColorName.Lucian) }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Swap),
            subtitle = evmLabelManager.mapped(record.exchangeAddress),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Swap(
                iconIn = TransactionViewItem.Icon.Regular(record.valueIn?.coinIconUrl, record.valueIn?.coinIconPlaceholder),
                iconOut = TransactionViewItem.Icon.Regular(record.valueOut?.coinIconUrl, record.valueOut?.coinIconPlaceholder),
            )
        )
    }

    private fun createViewItemFromEvmTransactionRecord(record: EvmTransactionRecord, progress: Float?, icon: TransactionViewItem.Icon?): TransactionViewItem {
        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Unknown),
            subtitle = Translator.getString(R.string.Transactions_Unknown_Description),
            primaryValue = null,
            secondaryValue = null,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Platform(record.source)
        )
    }

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        record: EvmOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = if (record.sentToSelf) {
            ColoredValue(getCoinString(record.value, true), ColorName.Leah)
        } else {
            getColoredValue(record.value, ColorName.Lucian)
        }

        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Send),
            subtitle = Translator.getString(R.string.Transactions_To, evmLabelManager.mapped(record.to)),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun getColoredValue(value: Any, color: ColorName): ColoredValue =
        when (value) {
            is TransactionValue -> ColoredValue(getCoinString(value), if (value.zeroValue) ColorName.Grey else color)
            is CurrencyValue -> ColoredValue(getCurrencyString(value), if (value.value.compareTo(BigDecimal.ZERO) == 0) ColorName.Grey else color)
            else -> ColoredValue(value.toString(), color)
        }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        record: EvmIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = getColoredValue(record.value, ColorName.Remus)
        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Receive),
            subtitle = Translator.getString(
                R.string.Transactions_From,
                evmLabelManager.mapped(record.from)
            ),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun createViewItemFromContractCreationTransactionRecord(
        record: ContractCreationTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_ContractCreation),
            subtitle = "---",
            primaryValue = null,
            secondaryValue = null,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Platform(record.source)
        )
    }

    private fun createViewItemFromContractCallTransactionRecord(
        record: ContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val (incomingValues, outgoingValues) = record.combined(record.incomingEvents, record.outgoingEvents)
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?, eventIcon) = getValues(incomingValues, outgoingValues, currencyValue)
        val title = record.method ?: Translator.getString(R.string.Transactions_ContractCall)

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = title,
            subtitle = evmLabelManager.mapped(record.contractAddress),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: eventIcon ?: TransactionViewItem.Icon.Platform(record.source)
        )
    }

    private fun createViewItemFromExternalContractCallTransactionRecord(
        record: ExternalContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val (incomingValues, outgoingValues) = record.combined(record.incomingEvents, record.outgoingEvents)
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?, eventIcon) = getValues(incomingValues, outgoingValues, currencyValue)

        val title: String
        val subTitle: String
        if (outgoingValues.isEmpty()) {
            title = Translator.getString(R.string.Transactions_Receive)
            val addresses = record.incomingEvents.mapNotNull { it.address }.toSet().toList()

            subTitle = if (addresses.size == 1) evmLabelManager.mapped(addresses.first()) else Translator.getString(R.string.Transactions_Multiple)
        } else {
            title = Translator.getString(R.string.Transactions_ExternalContractCall)
            subTitle = "---"
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = title,
            subtitle = subTitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: eventIcon ?: TransactionViewItem.Icon.Platform(record.source)
        )
    }

    private fun createViewItemFromBitcoinOutgoingTransactionRecord(
        record: BitcoinOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val subtitle = record.to?.let {
            Translator.getString(
                R.string.Transactions_To,
                evmLabelManager.mapped(it)
            )
        } ?: "---"

        val primaryValue = if (record.sentToSelf) {
            ColoredValue(getCoinString(record.value, true), ColorName.Leah)
        } else {
            getColoredValue(record.value, ColorName.Lucian)
        }

        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Send),
            subtitle = subtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            doubleSpend = record.conflictingHash != null,
            locked = locked,
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun createViewItemFromBitcoinIncomingTransactionRecord(
        record: BitcoinIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val subtitle = record.from?.let {
            Translator.getString(
                R.string.Transactions_From,
                evmLabelManager.mapped(it)
            )
        } ?: "---"

        val primaryValue = getColoredValue(record.value, ColorName.Remus)
        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        val lockState = record.lockState(lastBlockTimestamp)
        val locked = when {
            lockState == null -> null
            lockState.locked -> true
            else -> false
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Receive),
            subtitle = subtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            sentToSelf = false,
            doubleSpend = record.conflictingHash != null,
            locked = locked,
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun createViewItemFromBinanceChainOutgoingTransactionRecord(
        record: BinanceChainOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = if (record.sentToSelf) {
            ColoredValue(getCoinString(record.value, true), ColorName.Leah)
        } else {
            getColoredValue(record.value, ColorName.Lucian)
        }

        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Send),
            subtitle = Translator.getString(R.string.Transactions_To, evmLabelManager.mapped(record.to)),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun createViewItemFromBinanceChainIncomingTransactionRecord(
        record: BinanceChainIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = getColoredValue(record.value, ColorName.Remus)
        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Receive),
            subtitle = Translator.getString(
                R.string.Transactions_From,
                evmLabelManager.mapped(record.from)
            ),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        record: ApproveTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        lastBlockTimestamp: Long?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValueText: String
        val secondaryValueText: String?

        if (record.value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText = if (record.value.coinCode.isEmpty()) "" else Translator.getString(R.string.Transaction_Unlimited, record.value.coinCode)
        } else {
            primaryValueText = getCoinString(record.value, hideSign = true)
            secondaryValueText = currencyValue?.let { getCurrencyString(it) }
        }

        val primaryValue = ColoredValue(primaryValueText, ColorName.Leah)
        val secondaryValue = secondaryValueText?.let { ColoredValue(it, ColorName.Grey) }

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Approve),
            subtitle = evmLabelManager.mapped(record.spender),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: TransactionViewItem.Icon.Regular(record.value.coinIconUrl, record.value.coinIconPlaceholder)
        )
    }

    private fun getValues(incomingValues: List<TransactionValue>, outgoingValues: List<TransactionValue>, currencyValue: CurrencyValue?): Triple<ColoredValue, ColoredValue?, TransactionViewItem.Icon?> {
        val primaryValue: ColoredValue?
        val secondaryValue: ColoredValue?
        var icon: TransactionViewItem.Icon? = null

        when {
            // incoming
            (incomingValues.size == 1 && outgoingValues.isEmpty()) -> {
                val transactionValue = incomingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Remus)
                secondaryValue = currencyValue?.let {
                    getColoredValue(it, ColorName.Grey)
                }
                icon = TransactionViewItem.Icon.Regular(
                    url = transactionValue.coinIconUrl,
                    placeholder = transactionValue.coinIconPlaceholder
                )
            }

            // outgoing
            (incomingValues.isEmpty() && outgoingValues.size == 1) -> {
                val transactionValue = outgoingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Lucian)
                secondaryValue = currencyValue?.let {
                    getColoredValue(it, ColorName.Grey)
                }
                icon = TransactionViewItem.Icon.Regular(
                    url = transactionValue.coinIconUrl,
                    placeholder = transactionValue.coinIconPlaceholder
                )
            }

            // swap
            (incomingValues.size == 1 && outgoingValues.size == 1) -> {
                val inTransactionValue = incomingValues.first()
                val outTransactionValue = outgoingValues.first()
                primaryValue = getColoredValue(inTransactionValue, ColorName.Remus)
                secondaryValue = getColoredValue(outTransactionValue, ColorName.Lucian)
            }

            // outgoing multiple
            (incomingValues.isEmpty() && outgoingValues.isNotEmpty()) -> {
                primaryValue = getColoredValue(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Lucian)
                secondaryValue = getColoredValue(Translator.getString(R.string.Transactions_Multiple), ColorName.Grey)
            }

            // incoming multiple
            (incomingValues.isNotEmpty() && outgoingValues.isEmpty()) -> {
                primaryValue = getColoredValue(incomingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Remus)
                secondaryValue = getColoredValue(Translator.getString(R.string.Transactions_Multiple), ColorName.Grey)
            }

            else -> {
                primaryValue = getColoredValue(incomingValues.joinToString(", ") { it.coinCode }, ColorName.Remus)
                secondaryValue = getColoredValue(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Lucian)
            }
        }
        return Triple(primaryValue, secondaryValue, icon)
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiatFull(currencyValue.value.abs(), currencyValue.currency.symbol)
    }

    private fun getCoinString(transactionValue: TransactionValue, hideSign: Boolean = false): String {
        return transactionValue.decimalValue?.let { decimalValue ->
            val sign = when {
                hideSign -> ""
                decimalValue < BigDecimal.ZERO -> "-"
                decimalValue > BigDecimal.ZERO -> "+"
                else -> ""
            }
            sign + App.numberFormatter.formatCoinShort(
                decimalValue.abs(),
                transactionValue.coinCode,
                transactionValue.decimals ?: 8,
            )
        } ?: ""
    }

}