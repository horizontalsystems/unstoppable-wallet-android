package io.horizontalsystems.bankwallet.modules.transactions.q

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper

class TransactionViewItem2Factory {

    fun convertToViewItem(transactionItem: TransactionItem): TransactionViewItem2 = when (val record = transactionItem.record) {
        is ApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is BinanceChainIncomingTransactionRecord -> createViewItemFromBinanceChainIncomingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is BinanceChainOutgoingTransactionRecord -> createViewItemFromBinanceChainOutgoingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is BitcoinIncomingTransactionRecord -> createViewItemFromBitcoinIncomingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is BitcoinOutgoingTransactionRecord -> createViewItemFromBitcoinOutgoingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is ContractCallTransactionRecord -> createViewItemFromContractCallTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is EvmIncomingTransactionRecord -> createViewItemFromEvmIncomingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is EvmOutgoingTransactionRecord -> createViewItemFromEvmOutgoingTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(record, transactionItem.xxxCurrencyValue, transactionItem.lockState)
        else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
    }

    private fun createViewItemFromSwapTransactionRecord(
        record: SwapTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValue = ColoredValue(getCoinString(record.valueIn), R.color.jacob)
        val secondaryValue = record.valueOut?.let {
            ColoredValue(
                getCoinString(it),
                if (record.foreignRecipient) R.color.grey else R.color.remus
            )
        }

        return TransactionViewItem2(
            R.drawable.ic_tx_swap_20,
            null,
            Translator.getString(R.string.Transactions_Swap),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.exchangeAddress)
            ),
            primaryValue,
            secondaryValue,
            null,
            false,
            false
        )
    }

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        record: EvmOutgoingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_outgoing_20,
            null,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            null,
            record.sentToSelf,
            false
        )
    }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        record: EvmIncomingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_incoming_20,
            null,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            null,
            false,
            false
        )
    }

    private fun createViewItemFromContractCreationTransactionRecord(
        record: ContractCreationTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        return TransactionViewItem2(
            R.drawable.ic_tx_unordered,
            null,
            Translator.getString(R.string.Transactions_ContractCreation),
            "---",
            null,
            null,
            null,
            false,
            false
        )
    }

    private fun createViewItemFromContractCallTransactionRecord(
        record: ContractCallTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        return TransactionViewItem2(
            R.drawable.ic_tx_unordered,
            null,
            record.blockchainTitle + " " + Translator.getString(R.string.Transactions_ContractCall),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.contractAddress)
            ),
            null,
            null,
            false,
            false,
            false
        )
    }

    private fun createViewItemFromBitcoinOutgoingTransactionRecord(
        record: BitcoinOutgoingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val subtitle = record.to?.let {
            Translator.getString(
                R.string.Transactions_To,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_outgoing_20,
            null,
            Translator.getString(R.string.Transactions_Send),
            subtitle,
            primaryValue,
            secondaryValue,
            lockState?.locked,
            record.sentToSelf,
            record.conflictingHash != null
        )
    }

    private fun createViewItemFromBitcoinIncomingTransactionRecord(
        record: BitcoinIncomingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val subtitle = record.from?.let {
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(it)
            )
        } ?: "---"

        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_incoming_20,
            null,
            Translator.getString(R.string.Transactions_Receive),
            subtitle,
            primaryValue,
            secondaryValue,
            lockState?.locked,
            false,
            record.conflictingHash != null
        )
    }

    private fun createViewItemFromBinanceChainOutgoingTransactionRecord(
        record: BinanceChainOutgoingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.jacob
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_outgoing_20,
            null,
            Translator.getString(R.string.Transactions_Send),
            Translator.getString(R.string.Transactions_To, getNameOrAddressTruncated(record.to)),
            primaryValue,
            secondaryValue,
            false,
            record.sentToSelf,
            false
        )
    }

    private fun createViewItemFromBinanceChainIncomingTransactionRecord(
        record: BinanceChainIncomingTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValue = xxxCurrencyValue?.let {
            ColoredValue(
                getCurrencyString(it),
                R.color.remus
            )
        }
        val secondaryValue = ColoredValue(getCoinString(record.value), R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_incoming_20,
            null,
            Translator.getString(R.string.Transactions_Receive),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.from)
            ),
            primaryValue,
            secondaryValue,
            false,
            false,
            false
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        record: ApproveTransactionRecord,
        xxxCurrencyValue: CurrencyValue?,
        lockState: TransactionLockState?
    ): TransactionViewItem2 {
        val primaryValueText: String?
        val secondaryValueText: String

        if (record.value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText =
                Translator.getString(R.string.Transaction_Unlimited, record.value.coin.code)
        } else {
            primaryValueText = xxxCurrencyValue?.let { getCurrencyString(it) }
            secondaryValueText = getCoinString(record.value)
        }

        val primaryValue = primaryValueText?.let { ColoredValue(it, R.color.leah) }
        val secondaryValue = ColoredValue(secondaryValueText, R.color.grey)

        return TransactionViewItem2(
            R.drawable.ic_tx_checkmark_20,
            null,
            Translator.getString(R.string.Transactions_Approve),
            Translator.getString(
                R.string.Transactions_From,
                getNameOrAddressTruncated(record.spender)
            ),
            primaryValue,
            secondaryValue,
            false,
            false,
            false
        )
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiat(currencyValue.value.abs(), currencyValue.currency.symbol, 0, 2)
    }

    private fun getCoinString(coinValue: CoinValue): String {
        val significantDecimal = App.numberFormatter.getSignificantDecimalCoin(coinValue.value)
        return App.numberFormatter.formatCoin(
            coinValue.value.abs(),
            coinValue.coin.code,
            0,
            significantDecimal
        )
    }

    private fun getNameOrAddressTruncated(address: String): String {
        return TransactionInfoAddressMapper.title(address) ?: "${address.take(5)}...${address.takeLast(5)}"
    }
}