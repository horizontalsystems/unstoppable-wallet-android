package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoButtonType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.views.ListPosition.*
import java.math.BigDecimal
import java.util.*

class TransactionInfoViewItemFactory(
    private val numberFormatter: IAppNumberFormatter,
    private val translator: Translator,
    private val dateHelper: DateHelper,
    private val transactionInfoAddressMapper: TransactionInfoAddressMapper
) {

    fun getMiddleSectionItems(
        transaction: TransactionRecord,
        rates: Map<Coin, CurrencyValue>,
        lastBlockInfo: LastBlockInfo?,
        explorerData: TransactionInfoModule.ExplorerData
    ): List<TransactionInfoViewItem?> {

        val status = getStatusViewItem(transaction, lastBlockInfo?.height)
        val statusViewItem =
            TransactionInfoViewItem(Status(getString(R.string.TransactionInfo_Status), status))
        val date = dateHelper.getFullDate(Date(transaction.timestamp * 1000))
        val items = mutableListOf<TransactionInfoViewItem?>()

        items.add(
            TransactionInfoViewItem(
                Value(getString(R.string.TransactionInfo_Date), date),
                First
            )
        )

        when (transaction) {
            is EvmIncomingTransactionRecord -> {
                items.add(statusViewItem)

                rates[transaction.value.coin]?.let {
                    items.add(getHistoricalRateViewItem(it, transaction.value))
                }

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_From),
                            transaction.from
                        )
                    )
                )
                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_Id),
                            transaction.transactionHash,
                            true
                        ), Last
                    )
                )

                items.addAll(
                    0,
                    getActionsSection(
                        getString(R.string.TransactionInfo_Status_Received),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }
            is EvmOutgoingTransactionRecord -> {
                items.add(statusViewItem)

                items.add(getFeeViewItem(rates[transaction.value.coin], transaction.fee))

                rates[transaction.value.coin]?.let {
                    items.add(getHistoricalRateViewItem(it, transaction.value))
                }

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_To),
                            transaction.to
                        )
                    )
                )
                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_Id),
                            transaction.transactionHash,
                            true
                        ), Last
                    )
                )

                items.addAll(
                    0,
                    getActionsSection(
                        getString(R.string.TransactionInfo_Status_Sent),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }
            is SwapTransactionRecord -> {
                items.add(statusViewItem)

                items.add(getFeeViewItem(rates[transaction.fee.coin], transaction.fee))

                transaction.valueOut?.let { out ->
                    if (out.value > BigDecimal.ZERO) {
                        val price = transaction.valueIn.value / out.value
                        val priceValue = numberFormatter.formatCoin(price, out.coin.code, 0, 8)
                        items.add(
                            TransactionInfoViewItem(
                                Value(
                                    getString(R.string.TransactionInfo_Price),
                                    "${transaction.valueIn.coin.code} = $priceValue"
                                )
                            )
                        )
                    }
                }

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_Id),
                            transaction.transactionHash,
                            true
                        ), Last
                    )
                )

                items.addAll(
                    0,
                    getActionsSection(
                        getString(R.string.TransactionInfo_YouPaid),
                        transaction.valueIn,
                        rates[transaction.valueIn.coin],
                        false
                    )
                )

                transaction.valueOut?.let {
                    items.addAll(
                        0,
                        getActionsSection(
                            getString(R.string.TransactionInfo_YouGot),
                            transaction.valueOut,
                            rates[transaction.valueOut.coin],
                            true
                        )
                    )
                }

                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }
            is ApproveTransactionRecord -> {
                val rate = rates[transaction.value.coin]
                items.add(statusViewItem)

                items.add(getFeeViewItem(rates[transaction.fee.coin], transaction.fee))

                rate?.let {
                    items.add(getHistoricalRateViewItem(it, transaction.value))
                }

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_To),
                            transaction.spender
                        )
                    )
                )

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_Id),
                            transaction.transactionHash,
                            true
                        ), Last
                    )
                )

                items.addAll(
                    0,
                    getActionsSection(
                        getString(R.string.TransactionInfo_Status_Approved),
                        transaction.value,
                        rate,
                        null
                    )
                )

                items.add(null)
                items.addAll(getAdditionalButtons(explorerData, isApproval = true))

                return items
            }
            is ContractCallTransactionRecord -> {
                val topSectionItems = mutableListOf<TransactionInfoViewItem?>()
                topSectionItems.add(
                    TransactionInfoViewItem(
                        TransactionType(
                            getString(R.string.TransactionInfo_ContractCall),
                            getNameOrAddress(transaction.contractAddress)
                        ), Single
                    )
                )
                topSectionItems.add(null)

                if (transaction.outgoingEip20Events.size > 0) {
                    topSectionItems.add(
                        TransactionInfoViewItem(
                            TransactionType(
                                getString(R.string.TransactionInfo_YouPaid),
                                null
                            ), First
                        )
                    )

                    transaction.outgoingEip20Events.forEachIndexed { index, (_, eventCoinValue) ->
                        topSectionItems.add(
                            getAmountViewItem(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                false,
                                if (index == transaction.outgoingEip20Events.size - 1) Last else Middle
                            )
                        )
                    }

                    topSectionItems.add(null)
                }

                if (transaction.incomingEip20Events.size > 0 || transaction.incomingInternalETHs.size > 0) {
                    topSectionItems.add(
                        TransactionInfoViewItem(
                            TransactionType(
                                getString(R.string.TransactionInfo_YouGot),
                                null
                            ), First
                        )
                    )

                    transaction.incomingInternalETHs.firstOrNull()?.let { (_, coinValue) ->
                        val ethCoin = coinValue.coin
                        val ethSum =
                            transaction.incomingInternalETHs.sumOf { (_, eventCoinValue) -> eventCoinValue.value }

                        topSectionItems.add(
                            getAmountViewItem(
                                rates[ethCoin],
                                CoinValue(ethCoin, ethSum),
                                true
                            )
                        )
                    }

                    transaction.incomingEip20Events.forEach { (_, eventCoinValue) ->
                        topSectionItems.add(
                            getAmountViewItem(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                true
                            )
                        )
                    }

                    if (topSectionItems.size > 1) {
                        topSectionItems[topSectionItems.size - 1]?.listPosition = Last
                    }

                    topSectionItems.add(null)
                }

                items.addAll(0, topSectionItems)

                items.add(statusViewItem)

                items.add(getFeeViewItem(rates[transaction.fee.coin], transaction.fee))

                items.add(
                    TransactionInfoViewItem(
                        Decorated(
                            getString(R.string.TransactionInfo_Id),
                            transaction.transactionHash,
                            true
                        ), Last
                    )
                )

                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            else -> return emptyList()
        }
    }

    private fun getNameOrAddress(address: String): String {
        return transactionInfoAddressMapper.title(address)
            ?: "${address.take(5)}...${address.takeLast(5)}"
    }

    private fun getAdditionalButtons(
        explorerData: TransactionInfoModule.ExplorerData,
        isApproval: Boolean = false,
        isResend: Boolean = false
    ): List<TransactionInfoViewItem?> {
        val items = mutableListOf<TransactionInfoViewItem?>()
        val title = translator.getString(
            R.string.TransactionInfo_ButtonViewOnExplorerName,
            explorerData.title
        )
        items.add(
            TransactionInfoViewItem(
                Button(
                    title,
                    R.drawable.ic_language,
                    OpenExplorer(explorerData.url)
                ),
                Single
            )
        )
        items.add(null)

        if (isApproval) {
            items.add(
                TransactionInfoViewItem(
                    Button(
                        getString(R.string.TransactionInfo_RevokeApproval),
                        R.drawable.ic_close_20,
                        RevokeApproval
                    ),
                    Single
                )
            )
            items.add(null)
        }

        if (isResend) {
            items.add(
                TransactionInfoViewItem(
                    Button(
                        getString(R.string.TransactionInfo_Resend),
                        R.drawable.ic_resend_20,
                        Resend
                    ),
                    Single
                )
            )
            items.add(null)
        }

        return items
    }

    private fun getActionsSection(
        title: String,
        coinValue: CoinValue,
        rate: CurrencyValue?,
        incoming: Boolean?
    ): List<TransactionInfoViewItem?> {
        val items = mutableListOf<TransactionInfoViewItem?>()

        items.add(TransactionInfoViewItem(TransactionType(title, coinValue.coin.title), First))

        items.add(getAmountViewItem(rate, coinValue, incoming, Last))

        items.add(null) //add divider

        return items
    }

    private fun getAmountColor(incoming: Boolean?): Int {
        return when (incoming) {
            true -> R.color.remus
            false -> R.color.jacob
            else -> R.color.oz
        }
    }

    private fun getString(resId: Int): String {
        return translator.getString(resId)
    }

    private fun getStatusViewItem(
        transaction: TransactionRecord,
        lastBlockHeight: Int?
    ): TransactionStatusViewItem {

        return when (val status = transaction.status(lastBlockHeight)) {
            TransactionStatus.Failed -> Failed
            TransactionStatus.Pending -> Pending(getString(R.string.TransactionInfo_Status_Pending))
            TransactionStatus.Completed -> Completed(getCompletedStatusName(transaction))
            is TransactionStatus.Processing -> Processing(
                status.progress,
                getProcessingStatusName(transaction)
            )
        }
    }

    private fun getCompletedStatusName(transaction: TransactionRecord): String {
        return when (transaction) {
            is EvmIncomingTransactionRecord -> getString(R.string.TransactionInfo_Status_Received)
            is EvmOutgoingTransactionRecord -> getString(R.string.TransactionInfo_Status_Sent)
            is SwapTransactionRecord -> getString(R.string.TransactionInfo_Status_Swapped)
            is ApproveTransactionRecord -> getString(R.string.TransactionInfo_Status_Approved)
            is ContractCallTransactionRecord -> getString(R.string.TransactionInfo_Status_Confirmed)
            else -> ""
        }
    }

    private fun getProcessingStatusName(transaction: TransactionRecord): String {
        return when (transaction) {
            is EvmIncomingTransactionRecord -> getString(R.string.TransactionInfo_Status_Receiving)
            is EvmOutgoingTransactionRecord -> getString(R.string.TransactionInfo_Status_Sending)
            is SwapTransactionRecord -> getString(R.string.TransactionInfo_Status_Swapping)
            is ApproveTransactionRecord -> getString(R.string.TransactionInfo_Status_Approving)
            is ContractCallTransactionRecord -> getString(R.string.TransactionInfo_Status_Pending)
            else -> ""
        }
    }

    private fun getAmountViewItem(
        rate: CurrencyValue?,
        coinValue: CoinValue,
        incoming: Boolean?,
        listPosition: ListPosition = Middle
    ): TransactionInfoViewItem {
        val valueInFiat = rate?.let {
            numberFormatter.formatFiat(
                (it.value * coinValue.value).abs(),
                it.currency.symbol,
                0,
                2
            )
        } ?: "---"
        val coinValueFormatted =
            numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8)
        val coinValueColored = ColoredValue(coinValueFormatted, getAmountColor(incoming))

        return TransactionInfoViewItem(Amount(valueInFiat, coinValueColored), listPosition)
    }

    private fun getHistoricalRateViewItem(
        rate: CurrencyValue,
        coinValue: CoinValue,
    ): TransactionInfoViewItem {
        val rateFormatted =
            numberFormatter.formatFiat(rate.value, rate.currency.symbol, 2, 4)
        val rateValue = translator.getString(
            R.string.Balance_RatePerCoin,
            rateFormatted,
            coinValue.coin.code
        )
        return TransactionInfoViewItem(
            Value(
                getString(R.string.TransactionInfo_HistoricalRate),
                rateValue
            )
        )
    }

    private fun getFeeViewItem(
        rate: CurrencyValue?,
        coinValue: CoinValue
    ): TransactionInfoViewItem {

        val feeInFiat = rate?.let {
            numberFormatter.formatFiat(
                it.value * coinValue.value,
                it.currency.symbol,
                2,
                4
            )
        }
        val feeInCoin =
            numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8)

        val feeText = feeInCoin + (if (feeInFiat != null) " | $feeInFiat" else "")

        return TransactionInfoViewItem(Value(getString(R.string.TransactionInfo_Fee), feeText))
    }

}
