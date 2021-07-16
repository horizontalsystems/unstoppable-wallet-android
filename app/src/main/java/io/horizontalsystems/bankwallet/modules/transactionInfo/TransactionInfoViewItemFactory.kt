package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoActionButton.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoButtonType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.helpers.DateHelper
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

        val statusType = Status(
            getString(R.string.TransactionInfo_Status),
            getStatusViewItem(transaction, lastBlockInfo?.height)
        )
        val date = Value(
            getString(R.string.TransactionInfo_Date),
            dateHelper.getFullDate(Date(transaction.timestamp * 1000))
        )
        val items = mutableListOf<TransactionInfoViewItem?>()

        when (transaction) {
            is EvmIncomingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_From),
                        transaction.from
                    )
                )
                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Receive),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is EvmOutgoingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)
                middleSectionTypes.add(getFee(rates[transaction.value.coin], transaction.fee))

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_To),
                        transaction.to
                    )
                )
                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Send),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is SwapTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)
                middleSectionTypes.add(getFee(rates[transaction.fee.coin], transaction.fee))

                transaction.valueOut?.let { out ->
                    if (out.value > BigDecimal.ZERO) {
                        val price = transaction.valueIn.value / out.value
                        val priceValue = numberFormatter.formatCoin(price, out.coin.code, 0, 8)
                        middleSectionTypes.add(
                            Value(
                                getString(R.string.TransactionInfo_Price),
                                "${transaction.valueIn.coin.code} = $priceValue"
                            )
                        )
                    }
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                items.addAll(
                    getActionsSection(
                        getString(R.string.TransactionInfo_YouPaid),
                        transaction.valueIn,
                        rates[transaction.valueIn.coin],
                        false
                    )
                )

                //Top section
                transaction.valueOut?.let {
                    items.addAll(
                        getActionsSection(
                            getString(R.string.TransactionInfo_YouGot),
                            transaction.valueOut,
                            rates[transaction.valueOut.coin],
                            true
                        )
                    )
                }

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is ApproveTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()
                val rate = rates[transaction.value.coin]

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)
                middleSectionTypes.add(getFee(rates[transaction.fee.coin], transaction.fee))

                rate?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(getString(R.string.TransactionInfo_To), transaction.spender)
                )

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                val valueInFiat = rate?.let {
                    numberFormatter.formatFiat(
                        (it.value * transaction.value.value).abs(),
                        it.currency.symbol,
                        0,
                        2
                    )
                } ?: "---"

                val coinValueFormatted =
                    numberFormatter.formatCoin(
                        transaction.value.value,
                        transaction.value.coin.code,
                        0,
                        8
                    )
                val coinValueColored = ColoredValue(coinValueFormatted, getAmountColor(null))

                val isMaxValue = transaction.value.isMaxValue
                val currencyAmount = if (isMaxValue) "∞" else valueInFiat
                val coinAmountColored = if (isMaxValue) ColoredValue(
                    getString(R.string.TransactionInfo_Unlimited),
                    getAmountColor(null)
                ) else coinValueColored

                //Top Section
                items.add(
                    TransactionInfoViewItem(
                        TransactionType(
                            getString(R.string.Transactions_Approve),
                            transaction.value.coin.title
                        ), First
                    )
                )
                items.add(TransactionInfoViewItem(Amount(currencyAmount, coinAmountColored), Last))
                items.add(null)

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData, isApproval = true))

                return items
            }

            is ContractCallTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                items.add(
                    TransactionInfoViewItem(
                        TransactionType(
                            getString(R.string.Transactions_ContractCall),
                            getNameOrAddress(transaction.contractAddress)
                        ), Single
                    )
                )
                items.add(null)

                if (transaction.outgoingEip20Events.size > 0) {
                    val youPaidSection = mutableListOf<TransactionInfoItemType>()
                    youPaidSection.add(
                        TransactionType(
                            getString(R.string.TransactionInfo_YouPaid),
                            null
                        )
                    )

                    transaction.outgoingEip20Events.forEachIndexed { index, (_, eventCoinValue) ->
                        youPaidSection.add(
                            getAmount(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                false
                            )
                        )
                    }

                    items.addAll(getViewItems(youPaidSection))
                    items.add(null)
                }

                if (transaction.incomingEip20Events.size > 0 || transaction.incomingInternalETHs.size > 0) {
                    val youGotSection = mutableListOf<TransactionInfoItemType>()
                    youGotSection.add(
                        TransactionType(
                            getString(R.string.TransactionInfo_YouGot),
                            null
                        )
                    )

                    transaction.incomingInternalETHs.firstOrNull()?.let { (_, coinValue) ->
                        val ethCoin = coinValue.coin
                        val ethSum =
                            transaction.incomingInternalETHs.sumOf { (_, eventCoinValue) -> eventCoinValue.value }

                        youGotSection.add(
                            getAmount(
                                rates[ethCoin],
                                CoinValue(ethCoin, ethSum),
                                true
                            )
                        )
                    }

                    transaction.incomingEip20Events.forEach { (_, eventCoinValue) ->
                        youGotSection.add(
                            getAmount(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                true
                            )
                        )
                    }

                    items.addAll(getViewItems(youGotSection))
                    items.add(null)
                }

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)
                middleSectionTypes.add(getFee(rates[transaction.fee.coin], transaction.fee))

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BitcoinIncomingTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                transaction.from?.let {
                    middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_From), it))
                }

                val lockState = transaction.lockState(lastBlockInfo?.timestamp)

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                if (transaction.showRawTransaction) {
                    middleSectionTypes.add(
                        RawTransaction(
                            getString(R.string.TransactionInfo_RawTransaction),
                            CopyButton
                        )
                    )
                }

                getLockStateItem(lockState)?.let {
                    middleSectionTypes.add(it)
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Receive),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BitcoinOutgoingTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                transaction.fee?.let {
                    middleSectionTypes.add(getFee(rates[transaction.value.coin], it))
                }

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                transaction.to?.let {
                    middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_To), it))
                }

                val lockState = transaction.lockState(lastBlockInfo?.timestamp)

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                getLockStateItem(lockState)?.let {
                    middleSectionTypes.add(it)
                }

                if (transaction.showRawTransaction) {
                    middleSectionTypes.add(
                        RawTransaction(
                            getString(R.string.TransactionInfo_RawTransaction),
                            CopyButton
                        )
                    )
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Send),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            else -> return emptyList()
        }
    }

    private fun getViewItems(viewItemTypes: MutableList<TransactionInfoItemType>): List<TransactionInfoViewItem> {
        return viewItemTypes.mapIndexed { index, itemType ->
            TransactionInfoViewItem(itemType, Companion.getListPosition(viewItemTypes.size, index))
        }
    }

    private fun getLockStateItem(lockState: TransactionLockState?): TransactionInfoItemType? {
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

        items.add(TransactionInfoViewItem(getAmount(rate, coinValue, incoming), Last))

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
            TransactionStatus.Pending -> Pending(getString(R.string.Transactions_Pending))
            TransactionStatus.Completed -> Completed(getString(R.string.Transactions_Completed))
            is TransactionStatus.Processing -> Processing(
                status.progress,
                getString(R.string.Transactions_Processing)
            )
        }
    }

    private fun getAmount(
        rate: CurrencyValue?,
        coinValue: CoinValue,
        incoming: Boolean?
    ): TransactionInfoItemType {
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

        return Amount(valueInFiat, coinValueColored)
    }

    private fun getHistoricalRate(
        rate: CurrencyValue,
        coinValue: CoinValue,
    ): TransactionInfoItemType {
        val rateFormatted =
            numberFormatter.formatFiat(rate.value, rate.currency.symbol, 2, 4)
        val rateValue = translator.getString(
            R.string.Balance_RatePerCoin,
            rateFormatted,
            coinValue.coin.code
        )
        return Value(getString(R.string.TransactionInfo_HistoricalRate), rateValue)
    }

    private fun getFee(rate: CurrencyValue?, coinValue: CoinValue): TransactionInfoItemType {
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

        return Value(getString(R.string.TransactionInfo_Fee), feeText)
    }

}
