package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoActionButton.CopyButton
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoActionButton.ShareButton
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoPositionedViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition.Companion
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.min

class TransactionInfoViewItemFactory(
    private val numberFormatter: IAppNumberFormatter,
    private val translator: Translator,
    private val dateHelper: DateHelper,
    private val evmLabelManager: EvmLabelManager
) {

    fun getViewItemSections(transactionItem: TransactionInfoItem): List<TransactionInfoPositionedViewItem?> {
        val transaction = transactionItem.record
        val rates = transactionItem.rates

        val status = transaction.status(transactionItem.lastBlockInfo?.height)
        val itemSections = mutableListOf<List<TransactionInfoViewItem>>()
        val miscItemsSection = mutableListOf<TransactionInfoViewItem>()

        var sentToSelf = false

        when (transaction) {
            is EvmIncomingTransactionRecord ->
                itemSections.add(getReceiveSectionItems(transaction.value.coinName, transaction.value, transaction.from, rates[transaction.value.coinUid]))

            is EvmOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(getSendSectionItems(transaction.value.coinName, transaction.value, transaction.to, rates[transaction.value.coinUid], transaction.sentToSelf))
            }

            is SwapTransactionRecord -> {
                val valueIn = transaction.valueIn
                val valueOut = transaction.valueOut

                itemSections.add(getSwapEventSectionItems(getYouPayString(status), valueIn, transaction.amountIn, rates[valueIn.coinUid], false))

                if (valueOut != null) {
                    val youGetSectionItems = getSwapEventSectionItems(getYouGetString(status), valueOut, transaction.amountOut, rates[valueOut.coinUid], true).toMutableList()

                    if (transaction.recipient != null) {
                        youGetSectionItems.add(
                            Decorated(getString(R.string.TransactionInfo_RecipientHash), transaction.recipient, evmLabelManager.mapped(transaction.recipient))
                        )
                    }

                    itemSections.add(youGetSectionItems)
                }

                itemSections.add(getSwapDetailsSectionItems(rates, transaction.exchangeAddress, valueOut, valueIn))
            }

            is UnknownSwapTransactionRecord -> {
                val valueIn = transaction.valueIn
                val valueOut = transaction.valueOut

                if (valueIn != null) {
                    itemSections.add(getSwapEventSectionItems(getYouPayString(status), valueIn, null, rates[valueIn.coinUid], false))
                }

                if (valueOut != null) {
                    itemSections.add(getSwapEventSectionItems(getYouGetString(status), valueOut, null, rates[valueOut.coinUid], true))
                }

                itemSections.add(getSwapDetailsSectionItems(rates, transaction.exchangeAddress, valueOut, valueIn))
            }

            is ApproveTransactionRecord ->
                itemSections.add(getApproveSectionItems(transaction.value, rates[transaction.value.coinUid], transaction.spender))

            is ContractCallTransactionRecord -> {
                itemSections.add(getContractMethodSectionItems(transaction))

                for (event in transaction.outgoingEvents) {
                    itemSections.add(getSendSectionItems(event.value.coinName, event.value, event.address, rates[event.value.coinUid]))
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(getReceiveSectionItems(event.value.coinName, event.value, event.address, rates[event.value.coinUid]))
                }
            }

            is ExternalContractCallTransactionRecord -> {
                for (event in transaction.outgoingEvents) {
                    itemSections.add(getSendSectionItems(event.value.coinName, event.value, event.address, rates[event.value.coinUid]))
                }

                for (event in transaction.incomingEvents) {
                    itemSections.add(getReceiveSectionItems(event.value.coinName, event.value, event.address, rates[event.value.coinUid]))
                }
            }

            is BitcoinIncomingTransactionRecord -> {
                itemSections.add(getReceiveSectionItems(transaction.value.coinName, transaction.value, transaction.from, rates[transaction.value.coinUid]))

                miscItemsSection.addAll(getBitcoinSectionItems(transaction, transactionItem.lastBlockInfo))
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is BitcoinOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(getSendSectionItems(transaction.value.coinName, transaction.value, transaction.to, rates[transaction.value.coinUid], transaction.sentToSelf))

                miscItemsSection.addAll(getBitcoinSectionItems(transaction, transactionItem.lastBlockInfo))
                addMemoItem(transaction.memo, miscItemsSection)
            }

            is BinanceChainIncomingTransactionRecord -> {
                itemSections.add(getReceiveSectionItems(transaction.value.coinName, transaction.value, transaction.from, rates[transaction.value.coinUid]))

                addMemoItem(transaction.memo, miscItemsSection)
            }

            is BinanceChainOutgoingTransactionRecord -> {
                sentToSelf = transaction.sentToSelf
                itemSections.add(getSendSectionItems(transaction.value.coinName, transaction.value, transaction.to, rates[transaction.value.coinUid], transaction.sentToSelf))

                addMemoItem(transaction.memo, miscItemsSection)
            }

            else -> {}
        }

        if (sentToSelf) {
            miscItemsSection.add(
                SentToSelf(getString(R.string.TransactionInfo_SentToSelfNote), R.drawable.ic_arrow_return_20)
            )
        }
        if (miscItemsSection.isNotEmpty()) {
            itemSections.add(miscItemsSection)
        }

        itemSections.add(getStatusSectionItems(transaction, status, rates))
        itemSections.add(getExplorerSectionItems(transactionItem.explorerData))

        return convertToViewItems(itemSections)
    }

    private fun addMemoItem(
        memo: String?,
        miscItemsSection: MutableList<TransactionInfoViewItem>
    ) {
        if (!memo.isNullOrBlank()) {
            miscItemsSection.add(
                Value(getString(R.string.TransactionInfo_Memo), memo)
            )
        }
    }

    private fun convertToViewItems(viewItemGroups: List<List<TransactionInfoViewItem>>): List<TransactionInfoPositionedViewItem?> {
        val viewItems: MutableList<TransactionInfoPositionedViewItem?> = mutableListOf()

        for (viewItemTypes in viewItemGroups) {
            val sectionViewItems = viewItemTypes.mapIndexed { index, itemType ->
                TransactionInfoPositionedViewItem(itemType, Companion.getListPosition(viewItemTypes.size, index))
            }

            if (sectionViewItems.isNotEmpty()) {
                viewItems.addAll(sectionViewItems)
                viewItems.add(null)
            }
        }

        return viewItems
    }

    private fun getReceiveSectionItems(coinName: String, value: TransactionValue, fromAddress: String?, coinPrice: CurrencyValue?): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            Transaction(getString(R.string.Transactions_Receive), coinName, R.drawable.ic_arrow_down_left_12),
            getAmount(coinPrice, value, true)
        )

        coinPrice?.let {
            items.add(getHistoricalRate(it, value))
        }

        fromAddress?.let {
            items.add(
                Decorated(
                    getString(R.string.TransactionInfo_From),
                    it,
                    evmLabelManager.mapped(it)
                )
            )
        }

        return items
    }

    private fun getSendSectionItems(coinName: String, value: TransactionValue, toAddress: String?, coinPrice: CurrencyValue?, sentToSelf: Boolean = false): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            Transaction(getString(R.string.Transactions_Send), coinName, R.drawable.ic_arrow_up_right_12),
            getAmount(coinPrice, value, if (sentToSelf) null else false)
        )

        coinPrice?.let {
            items.add(getHistoricalRate(it, value))
        }

        toAddress?.let {
            items.add(
                Decorated(
                    getString(R.string.TransactionInfo_To),
                    toAddress,
                    evmLabelManager.mapped(toAddress)
                )
            )
        }

        return items
    }

    private fun getSwapEventSectionItems(title: String, value: TransactionValue, amount: SwapTransactionRecord.Amount?, rate: CurrencyValue?, incoming: Boolean): List<TransactionInfoViewItem> =
        listOf(
            Transaction(
                title,
                value.coinName,
                icon = if (incoming)
                    R.drawable.ic_arrow_down_left_12
                else
                    R.drawable.ic_arrow_up_right_12
            ),
            getAmount(rate, value, incoming, amount)
        )

    private fun getSwapDetailsSectionItems(rates: Map<String, CurrencyValue>, exchangeAddress: String, valueOut: TransactionValue?, valueIn: TransactionValue?): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            Value(
                getString(R.string.TransactionInfo_Service),
                evmLabelManager.mapped(exchangeAddress)
            )
        )

        if (valueIn == null || valueOut == null) {
            return items
        }

        val decimalValueIn = valueIn.decimalValue
        val decimalValueOut = valueOut.decimalValue
        val valueOutDecimals = valueOut.decimals
        val valueInDecimals = valueIn.decimals

        if (decimalValueIn == null || decimalValueOut == null || valueInDecimals == null || valueOutDecimals == null) {
            return items
        }

        val priceValue = if (decimalValueOut.compareTo(BigDecimal.ZERO) == 0) {
            Translator.getString(R.string.NotAvailable)
        } else {
            val price = decimalValueIn.divide(
                decimalValueOut,
                min(valueOutDecimals, valueInDecimals),
                RoundingMode.HALF_EVEN
            ).abs()
            val formattedPrice = numberFormatter.formatCoin(price, valueIn.coinCode, 0, 8)
            val formattedFiatPrice = rates[valueIn.coinUid]?.let { rate ->
                numberFormatter.formatFiat(price * rate.value, rate.currency.symbol, 0, 2).let {
                    " ($it)"
                }
            } ?: ""
            "${valueOut.coinCode} = $formattedPrice$formattedFiatPrice"
        }

        items.add(
            Value(
                getString(R.string.TransactionInfo_Price),
                priceValue
            )
        )

        return items
    }

    private fun getApproveSectionItems(value: TransactionValue, coinPrice: CurrencyValue?, spenderAddress: String): List<TransactionInfoViewItem> {
        val fiatAmountFormatted = coinPrice?.let {
            value.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiat(
                    (it.value * decimalValue).abs(),
                    it.currency.symbol,
                    0,
                    2
                )
            }
        } ?: "---"

        val coinAmountFormatted = value.decimalValue?.let { decimalValue ->
            numberFormatter.formatCoin(
                decimalValue,
                value.coinCode,
                0,
                8
            )
        } ?: ""

        val coinAmountString = if (value.isMaxValue) translator.getString(
            R.string.Transaction_Unlimited,
            value.coinCode
        ) else coinAmountFormatted

        val coinAmountColoredValue = ColoredValue(coinAmountString,  getAmountColor(null))
        val fiatAmountColoredValue = ColoredValue(
            if (value.isMaxValue) "∞" else fiatAmountFormatted,
            R.color.grey
        )

        return listOf(
            Transaction(getString(R.string.Transactions_Approve), value.coinName, R.drawable.ic_checkmark_24),
            Amount(value.coinIconUrl, value.coinIconPlaceholder, coinAmountColoredValue, fiatAmountColoredValue),
            Decorated(getString(R.string.TransactionInfo_Spender), spenderAddress, evmLabelManager.mapped(spenderAddress))
        )
    }

    private fun getContractMethodSectionItems(transaction: ContractCallTransactionRecord) =
        listOf(
            Transaction(
                transaction.method ?: getString(R.string.Transactions_ContractCall),
                evmLabelManager.mapped(transaction.contractAddress),
                TransactionViewItem.Icon.Platform(transaction.source).iconRes
            )
        )

    private fun getBitcoinSectionItems(transaction: BitcoinTransactionRecord, lastBlockInfo: LastBlockInfo?): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf()

        transaction.conflictingHash?.let { conflictingHash ->
            items.add(
                getDoubleSpendViewItem(
                    transaction.transactionHash,
                    conflictingHash
                )
            )
        }

        if (transaction.showRawTransaction) {
            items.add(
                RawTransaction(
                    getString(R.string.TransactionInfo_RawTransaction),
                    CopyButton
                )
            )
        }

        val lockState = transaction.lockState(lastBlockInfo?.timestamp)
        getLockStateItem(lockState)?.let {
            items.add(it)
        }

        return items
    }

    private fun getStatusSectionItems(transaction: TransactionRecord, status: TransactionStatus, rates: Map<String, CurrencyValue?>): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            Value(
                getString(R.string.TransactionInfo_Date),
                dateHelper.getFullDate(Date(transaction.timestamp * 1000))
            ),
            Status(
                getString(R.string.TransactionInfo_Status),
                R.drawable.ic_info_24,
                when (status) {
                    TransactionStatus.Failed -> Failed
                    TransactionStatus.Pending -> Pending(getString(R.string.Transactions_Pending))
                    TransactionStatus.Completed -> Completed(getString(R.string.Transactions_Completed))
                    is TransactionStatus.Processing -> Processing(
                        status.progress,
                        getString(R.string.Transactions_Processing)
                    )
                }
            )
        )

        getOptionsItem(status)?.let { items.add(it) }

        when (transaction) {
            is EvmTransactionRecord ->
                if (!transaction.foreignTransaction && transaction.fee != null) {
                    items.add(getEvmFeeItem(transaction.fee, rates[transaction.fee.coinUid], status))
                }

            is BitcoinOutgoingTransactionRecord ->
                transaction.fee?.let { items.add(getFee(it, rates[it.coinUid])) }

            is BinanceChainOutgoingTransactionRecord ->
                items.add(getFee(transaction.fee, rates[transaction.fee.coinUid]))
        }

        items.add(
            Decorated(
                getString(R.string.TransactionInfo_Id),
                transaction.transactionHash,
                transaction.transactionHash,
                ShareButton(transaction.transactionHash)
            )
        )

        return items
    }

    private fun getExplorerSectionItems(explorerData: TransactionInfoModule.ExplorerData): List<TransactionInfoViewItem> =
        listOf(
            Explorer(
                translator.getString(R.string.TransactionInfo_ButtonViewOnExplorerName, explorerData.title),
                explorerData.url
            )
        )

    private fun getDoubleSpendViewItem(transactionHash: String, conflictingHash: String) = DoubleSpend(
        getString(R.string.TransactionInfo_DoubleSpendNote),
        R.drawable.ic_double_spend_20,
        transactionHash,
        conflictingHash
    )

    private fun getYouPayString(status: TransactionStatus): String {
        return if (status == TransactionStatus.Completed) {
            getString(R.string.TransactionInfo_YouPaid)
        } else {
            getString(R.string.TransactionInfo_YouPay)
        }
    }

    private fun getYouGetString(status: TransactionStatus): String {
        return if (status == TransactionStatus.Completed) {
            getString(R.string.TransactionInfo_YouGot)
        } else {
            getString(R.string.TransactionInfo_YouGet)
        }
    }

    private fun getLockStateItem(lockState: TransactionLockState?): TransactionInfoViewItem? {
        return lockState?.let {
            val leftIcon = if (it.locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20
            val date = DateHelper.getFullDate(it.date)
            val title = translator.getString(
                if (it.locked) R.string.TransactionInfo_LockedUntil else R.string.TransactionInfo_UnlockedAt,
                date
            )
            LockState(title, leftIcon, it.date, it.locked)
        }
    }

    private fun getAmountColor(incoming: Boolean?): Int {
        return when (incoming) {
            true -> R.color.remus
            false -> R.color.lucian
            else -> R.color.leah
        }
    }

    private fun getString(resId: Int): String {
        return translator.getString(resId)
    }

    private fun getAmount(
        rate: CurrencyValue?,
        value: TransactionValue,
        incoming: Boolean?,
        amount: SwapTransactionRecord.Amount? = null
    ): TransactionInfoViewItem {
        val valueInFiat = rate?.let {
            value.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiat(
                    (it.value * decimalValue).abs(),
                    it.currency.symbol,
                    0,
                    2
                )
            }
        } ?: "---"
        val fiatValueColored = ColoredValue(valueInFiat, R.color.grey)
        val coinValueFormatted = value.decimalValue?.let { decimalValue ->
            val sign = when {
                incoming == null -> ""
                decimalValue < BigDecimal.ZERO -> "-"
                decimalValue > BigDecimal.ZERO -> "+"
                else -> ""
            }
            val valueWithCoinCode = numberFormatter.formatCoin(decimalValue.abs(), value.coinCode, 0, 8)
            if (amount is SwapTransactionRecord.Amount.Extremum && incoming != null) {
                val suffix = if (incoming) getString(R.string.Swap_AmountMin) else getString(R.string.Swap_AmountMax)
                "$sign$valueWithCoinCode $suffix"
            } else {
                "$sign$valueWithCoinCode"
            }
        } ?: "---"

        val coinValueColored = ColoredValue(coinValueFormatted, getAmountColor(incoming))
        return Amount(value.coinIconUrl, value.coinIconPlaceholder, coinValueColored, fiatValueColored)
    }

    private fun getHistoricalRate(
        rate: CurrencyValue,
        transactionValue: TransactionValue,
    ): TransactionInfoViewItem {
        val rateFormatted =
            numberFormatter.formatFiat(rate.value, rate.currency.symbol, 2, 4)
        val rateValue = translator.getString(
            R.string.Balance_RatePerCoin,
            rateFormatted,
            transactionValue.coinCode
        )
        return Value(getString(R.string.TransactionInfo_HistoricalRate), rateValue)
    }

    private fun getFee(transactionValue: TransactionValue, rate: CurrencyValue?): TransactionInfoViewItem {
        val feeAmountString = getFeeAmountString(rate, transactionValue)

        return Value(getString(R.string.TransactionInfo_Fee), feeAmountString)
    }

    private fun getEvmFeeItem(
        transactionValue: TransactionValue,
        rate: CurrencyValue?,
        status: TransactionStatus
    ): TransactionInfoViewItem {
        val feeAmountString = getFeeAmountString(rate, transactionValue)
        val feeTitle: String = when (status) {
            TransactionStatus.Pending -> getString(R.string.TransactionInfo_FeeEstimated)
            is TransactionStatus.Processing,
            TransactionStatus.Failed,
            TransactionStatus.Completed -> getString(R.string.TransactionInfo_Fee)
        }

        return Value(feeTitle, feeAmountString)
    }

    private fun getFeeAmountString(rate: CurrencyValue?, transactionValue: TransactionValue): String {
        val feeInFiat = rate?.let {
            transactionValue.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiat(
                    it.value * decimalValue,
                    it.currency.symbol,
                    2,
                    4
                )
            }
        }
        val feeInCoin = transactionValue.decimalValue?.let { decimalValue ->
            numberFormatter.formatCoin(decimalValue, transactionValue.coinCode, 0, 8)
        } ?: ""

        return feeInCoin + (if (feeInFiat != null) " | $feeInFiat" else "")
    }

    private fun getOptionsItem(status: TransactionStatus): TransactionInfoViewItem? =
        if (status == TransactionStatus.Pending) {
            Options(
                Translator.getString(R.string.TransactionInfo_Options),
                TransactionInfoOption(
                    Translator.getString(R.string.TransactionInfo_SpeedUp),
                    TransactionInfoOption.Type.SpeedUp
                ),
                TransactionInfoOption(
                    Translator.getString(R.string.TransactionInfo_Cancel),
                    TransactionInfoOption.Type.Cancel
                )
            )
        } else {
            null
        }

}
