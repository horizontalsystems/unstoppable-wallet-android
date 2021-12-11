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
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
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
            is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is EvmIncomingTransactionRecord -> createViewItemFromEvmIncomingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is EvmOutgoingTransactionRecord -> createViewItemFromEvmOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
            is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(record, transactionItem.currencyValue, progress, lastBlockTimestamp, icon)
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
        val primaryValue = ColoredValue(getCoinString(record.valueIn), R.color.jacob)
        val secondaryValue = record.valueOut?.let {
            ColoredValue(
                getCoinString(it),
                if (record.foreignRecipient) R.color.grey else R.color.remus
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

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        record: EvmOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Int?,
        lastBlockTimestamp: Long?,
        icon: Int?
    ): TransactionViewItem {
        val primaryValue = currencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
        return TransactionViewItem(
            record.uid,
            icon ?: R.drawable.ic_tx_unordered,
            progress,
            record.blockchainTitle + " " + Translator.getString(R.string.Transactions_ContractCall),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.contractAddress)
            ),
            null,
            null,
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
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

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
            secondaryValueText = record.value.coin?.let {
                Translator.getString(R.string.Transaction_Unlimited, it.code)
            } ?: ""
        } else {
            primaryValueText = currencyValue?.let { getCurrencyString(it) }
            secondaryValueText = getCoinString(record.value)
        }

        val primaryValue = primaryValueText?.let { ColoredValue(it, R.color.leah) }
        val secondaryValue = ColoredValue(secondaryValueText, R.color.grey)

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

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiat(currencyValue.value.abs(), currencyValue.currency.symbol, 0, 2)
    }

    private fun getCoinString(transactionValue: TransactionValue): String {
        return transactionValue.decimalValue?.let { decimalValue ->
            val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(decimalValue)
            App.numberFormatter.formatCoin(
                decimalValue.abs(),
                transactionValue.coin?.code ?: "",
                0,
                significantDecimal
            )
        } ?: ""
    }

    private fun getNameOrAddressTruncated(address: String): String {
        return TransactionInfoAddressMapper.title(address) ?: "${address.take(5)}...${address.takeLast(5)}"
    }
}