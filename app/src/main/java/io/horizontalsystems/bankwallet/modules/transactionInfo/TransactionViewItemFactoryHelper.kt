package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatSection
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.bankwallet.modules.transactions.TransactionViewItem
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import kotlin.math.min

object TransactionViewItemFactoryHelper {
    val zeroAddress = "0x0000000000000000000000000000000000000000"
    private val numberFormatter = App.numberFormatter
    private val contactsRepo = App.contactsRepository
    private val evmLabelManager = App.evmLabelManager

    fun getMemoItem(memo: String) =
        TransactionInfoViewItem.Value(Translator.getString(R.string.TransactionInfo_Memo), memo)

    private fun getFeeAmountString(
        rate: CurrencyValue?,
        transactionValue: TransactionValue,
    ): String {
        val feeInFiat = rate?.let {
            transactionValue.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiatFull(
                    it.value * decimalValue,
                    it.currency.symbol
                )
            }
        }
        val feeInCoin = transactionValue.decimalValue?.let { decimalValue ->
            numberFormatter.formatCoinFull(decimalValue, transactionValue.coinCode, 8)
        } ?: ""

        return feeInCoin + (if (feeInFiat != null) " | $feeInFiat" else "")
    }

    private fun getDoubleSpendViewItem(transactionHash: String, conflictingHash: String) =
        TransactionInfoViewItem.DoubleSpend(transactionHash, conflictingHash)

    private fun getLockStateItem(lockState: TransactionLockState?): TransactionInfoViewItem? {
        return lockState?.let {
            val leftIcon = if (it.locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20
            val date = DateHelper.getFullDate(it.date)
            val title = Translator.getString(
                if (it.locked) R.string.TransactionInfo_LockedUntil else R.string.TransactionInfo_UnlockedAt,
                date
            )
            TransactionInfoViewItem.LockState(title, leftIcon, it.date, it.locked)
        }
    }

    private fun getFee(
        transactionValue: TransactionValue,
        rate: CurrencyValue?,
    ): TransactionInfoViewItem {
        val feeAmountString = getFeeAmountString(rate, transactionValue)

        return TransactionInfoViewItem.Value(
            Translator.getString(R.string.TransactionInfo_Fee),
            feeAmountString
        )
    }

    private fun getFeeItem(
        transactionValue: TransactionValue,
        rate: CurrencyValue?,
        status: TransactionStatus,
    ): TransactionInfoViewItem {
        val feeAmountString = getFeeAmountString(rate, transactionValue)
        val feeTitle: String = when (status) {
            TransactionStatus.Pending -> Translator.getString(R.string.TransactionInfo_FeeEstimated)
            is TransactionStatus.Processing,
            TransactionStatus.Failed,
            TransactionStatus.Completed,
                -> Translator.getString(R.string.TransactionInfo_Fee)
        }

        return TransactionInfoViewItem.Value(feeTitle, feeAmountString)
    }

    private fun getContact(address: String?, blockchainType: BlockchainType): Contact? {
        return contactsRepo.getContactsFiltered(blockchainType, addressQuery = address)
            .firstOrNull()
    }

    private fun getAmountColor(incoming: Boolean?): ColorName {
        return when (incoming) {
            true -> ColorName.Remus
            false -> ColorName.Lucian
            else -> ColorName.Leah
        }
    }

    private fun getNftAmount(
        title: String,
        value: TransactionValue.NftValue,
        incoming: Boolean?,
        hideAmount: Boolean,
        nftMetadata: NftAssetBriefMetadata?,
    ): TransactionInfoViewItem {
        val valueFormatted = if (hideAmount) "*****" else value.decimalValue.let { decimalValue ->
            val sign = when {
                incoming == null -> ""
                decimalValue < BigDecimal.ZERO -> "-"
                decimalValue > BigDecimal.ZERO -> "+"
                else -> ""
            }
            val valueWithCoinCode =
                numberFormatter.formatCoinFull(decimalValue.abs(), value.coinCode, 8)
            "$sign$valueWithCoinCode"
        }

        val nftName = nftMetadata?.name ?: value.tokenName?.let {
            when (value.nftUid) {
                is NftUid.Evm -> "$it #${value.nftUid.tokenId}"
                is NftUid.Solana -> it
            }
        } ?: "#${value.nftUid.tokenId}"

        return TransactionInfoViewItem.NftAmount(
            title,
            ColoredValue(valueFormatted, getAmountColor(incoming)),
            nftName,
            nftMetadata?.previewImageUrl,
            R.drawable.icon_24_nft_placeholder,
            null,
        )
    }

    private fun getAmount(
        rate: CurrencyValue?,
        value: TransactionValue,
        incoming: Boolean?,
        hideAmount: Boolean,
        amountType: AmountType,
        amount: SwapTransactionRecord.Amount? = null,
        hasRecipient: Boolean = false,
    ): TransactionInfoViewItem {
        val valueInFiat = if (hideAmount) "*****" else rate?.let {
            value.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiatFull(
                    (it.value * decimalValue).abs(),
                    it.currency.symbol
                )
            }
        } ?: "---"
        val fiatValueColored = ColoredValue(valueInFiat, ColorName.Grey)
        val coinValueFormatted =
            if (hideAmount) "*****" else value.decimalValue?.let { decimalValue ->
                val sign = when (incoming) {
                    true -> "+"
                    false -> "-"
                    else -> ""
                }
                val valueWithCoinCode =
                    numberFormatter.formatCoinFull(decimalValue.abs(), value.coinCode, 8)
                if (amount is SwapTransactionRecord.Amount.Extremum && incoming != null) {
                    val suffix =
                        if (incoming) Translator.getString(R.string.Swap_AmountMin) else Translator.getString(
                            R.string.Swap_AmountMax
                        )
                    "$sign$valueWithCoinCode $suffix"
                } else {
                    "$sign$valueWithCoinCode"
                }
            } ?: "---"

        val color = if (hasRecipient && incoming == true) {
            ColorName.Lucian
        } else {
            getAmountColor(incoming)
        }

        val coinValueColored = ColoredValue(coinValueFormatted, color)
        val coinUid = if (value is TransactionValue.CoinValue && !value.token.isCustom) {
            value.token.coin.uid
        } else {
            null
        }
        return TransactionInfoViewItem.Amount(
            coinValueColored,
            fiatValueColored,
            value.coinIconUrl,
            value.alternativeCoinIconUrl,
            value.coinIconPlaceholder,
            coinUid,
            value.badge,
            amountType,
        )
    }

    private fun getHistoricalRate(
        rate: CurrencyValue?,
        transactionValue: TransactionValue,
    ): TransactionInfoViewItem {
        val rateValue = if (rate == null) {
            "---"
        } else {
            val rateFormatted = numberFormatter.formatFiatFull(rate.value, rate.currency.symbol)
            Translator.getString(
                R.string.Balance_RatePerCoin,
                rateFormatted,
                transactionValue.coinCode
            )
        }
        return TransactionInfoViewItem.Value(
            Translator.getString(R.string.TransactionInfo_HistoricalRate),
            rateValue
        )
    }

    fun getReceiveSectionItems(
        value: TransactionValue,
        fromAddress: String?,
        coinPrice: CurrencyValue?,
        hideAmount: Boolean,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf(),
        blockchainType: BlockchainType,
    ): List<TransactionInfoViewItem> {
        val mint = fromAddress == zeroAddress
        val title: String =
            if (mint) Translator.getString(R.string.Transactions_Mint) else Translator.getString(R.string.Transactions_Receive)

        val amount: TransactionInfoViewItem
        val rate: TransactionInfoViewItem?

        when (value) {
            is TransactionValue.NftValue -> {
                amount = getNftAmount(title, value, true, hideAmount, nftMetadata[value.nftUid])
                rate = null
            }

            else -> {
                amount = getAmount(coinPrice, value, true, hideAmount, AmountType.Received)
                rate = getHistoricalRate(coinPrice, value)
            }
        }

        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            amount
        )

        if (!mint && fromAddress != null) {
            val contact = getContact(fromAddress, blockchainType)
            items.add(
                TransactionInfoViewItem.Address(
                    Translator.getString(R.string.TransactionInfo_From),
                    fromAddress,
                    contact == null,
                    blockchainType,
                    StatSection.AddressFrom
                )
            )
            contact?.let {
                items.add(
                    TransactionInfoViewItem.ContactItem(it)
                )
            }
        }

        rate?.let { items.add(it) }

        return items
    }


    fun getSendSectionItems(
        value: TransactionValue,
        toAddress: String?,
        coinPrice: CurrencyValue?,
        hideAmount: Boolean,
        sentToSelf: Boolean = false,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf(),
        blockchainType: BlockchainType,
    ): List<TransactionInfoViewItem> {
        val burn = toAddress == zeroAddress

        val title: String =
            if (burn) Translator.getString(R.string.Transactions_Burn) else Translator.getString(R.string.Transactions_Send)

        val amount: TransactionInfoViewItem
        val rate: TransactionInfoViewItem?

        when (value) {
            is TransactionValue.NftValue -> {
                amount = getNftAmount(
                    title,
                    value,
                    if (sentToSelf) null else false,
                    hideAmount,
                    nftMetadata[value.nftUid]
                )
                rate = null
            }

            else -> {
                amount = getAmount(
                    coinPrice,
                    value,
                    if (sentToSelf) null else false,
                    hideAmount,
                    AmountType.Sent
                )
                rate = getHistoricalRate(coinPrice, value)
            }
        }

        val items: MutableList<TransactionInfoViewItem> = mutableListOf(amount)

        if (!burn && toAddress != null) {
            val contact = getContact(toAddress, blockchainType)
            items.add(
                TransactionInfoViewItem.Address(
                    Translator.getString(R.string.TransactionInfo_To),
                    toAddress,
                    contact == null,
                    blockchainType,
                    StatSection.AddressTo
                )
            )

            contact?.let {
                items.add(TransactionInfoViewItem.ContactItem(it))
            }
        }

        rate?.let { items.add(it) }

        return items
    }

    fun getSwapEventSectionItems(
        valueIn: TransactionValue?,
        valueOut: TransactionValue?,
        rates: Map<String, CurrencyValue>,
        amount: SwapTransactionRecord.Amount?,
        hideAmount: Boolean,
        hasRecipient: Boolean,
    ) = buildList {
        valueIn?.let {
            add(
                getAmount(
                    rates[valueIn.coinUid],
                    valueIn,
                    false,
                    hideAmount,
                    AmountType.YouSent,
                    amount
                )
            )
        }
        valueOut?.let {
            add(
                getAmount(
                    rates[valueOut.coinUid],
                    valueOut,
                    true,
                    hideAmount,
                    AmountType.YouGot,
                    hasRecipient = hasRecipient,
                )
            )
        }
    }

    fun getSwapDetailsSectionItems(
        rates: Map<String, CurrencyValue>,
        exchangeAddress: String,
        valueOut: TransactionValue?,
        valueIn: TransactionValue?,
    ): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            TransactionInfoViewItem.Value(
                Translator.getString(R.string.TransactionInfo_Service),
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

        val priceValueOne = if (decimalValueOut.compareTo(BigDecimal.ZERO) == 0) {
            Translator.getString(R.string.NotAvailable)
        } else {
            val price = decimalValueIn.divide(
                decimalValueOut,
                min(valueOutDecimals, valueInDecimals),
                RoundingMode.HALF_EVEN
            ).abs()
            val formattedPrice = numberFormatter.formatCoinFull(price, valueIn.coinCode, 8)
            val formattedFiatPrice = rates[valueIn.coinUid]?.let { rate ->
                numberFormatter.formatFiatFull(price * rate.value, rate.currency.symbol).let {
                    " ($it)"
                }
            } ?: ""
            "${valueOut.coinCode} = $formattedPrice$formattedFiatPrice"
        }

        val priceValueTwo = if (decimalValueIn.compareTo(BigDecimal.ZERO) == 0) {
            Translator.getString(R.string.NotAvailable)
        } else {
            val price = decimalValueOut.divide(
                decimalValueIn,
                min(valueInDecimals, valueOutDecimals),
                RoundingMode.HALF_EVEN
            ).abs()
            val formattedPrice = numberFormatter.formatCoinFull(price, valueOut.coinCode, 8)
            val formattedFiatPrice = rates[valueOut.coinUid]?.let { rate ->
                numberFormatter.formatFiatFull(price * rate.value, rate.currency.symbol).let {
                    " ($it)"
                }
            } ?: ""
            "${valueIn.coinCode} = $formattedPrice$formattedFiatPrice"
        }

        items.add(
            TransactionInfoViewItem.PriceWithToggle(
                Translator.getString(R.string.TransactionInfo_Price),
                priceValueOne,
                priceValueTwo,
            )
        )

        return items
    }

    fun getContractCreationItems(transaction: ContractCreationTransactionRecord): List<TransactionInfoViewItem> =
        listOf(
            TransactionInfoViewItem.Transaction(
                Translator.getString(R.string.Transactions_ContractCreation),
                "",
                TransactionViewItem.Icon.Platform(transaction.blockchainType).iconRes
            )
        )

    fun getApproveSectionItems(
        value: TransactionValue,
        coinPrice: CurrencyValue?,
        spenderAddress: String,
        hideAmount: Boolean,
        blockchainType: BlockchainType,
    ): List<TransactionInfoViewItem> {
        val fiatAmountFormatted = coinPrice?.let {
            value.decimalValue?.let { decimalValue ->
                numberFormatter.formatFiatFull(
                    (it.value * decimalValue).abs(),
                    it.currency.symbol
                )
            }
        } ?: "---"

        val coinAmountFormatted = value.decimalValue?.let { decimalValue ->
            numberFormatter.formatCoinFull(
                decimalValue,
                value.coinCode,
                8
            )
        } ?: ""

        val coinAmountString = when {
            hideAmount -> "*****"
            value.isMaxValue -> "∞ ${value.coinCode}"

            else -> coinAmountFormatted
        }

        val coinAmountColoredValue = ColoredValue(coinAmountString, getAmountColor(null))

        val fiatAmountString = when {
            hideAmount -> "*****"
            value.isMaxValue -> Translator.getString(R.string.Transaction_Unlimited)
            else -> fiatAmountFormatted
        }

        val fiatAmountColoredValue = ColoredValue(fiatAmountString, ColorName.Grey)

        val contact = getContact(spenderAddress, blockchainType)

        val items = mutableListOf(
            TransactionInfoViewItem.Amount(
                coinAmountColoredValue,
                fiatAmountColoredValue,
                value.coinIconUrl,
                value.alternativeCoinIconUrl,
                value.coinIconPlaceholder,
                value.coin?.uid,
                value.badge,
                AmountType.Approved
            ),
            TransactionInfoViewItem.Address(
                Translator.getString(R.string.TransactionInfo_Spender),
                spenderAddress,
                contact == null,
                blockchainType,
                StatSection.AddressSpender
            )
        )

        contact?.let {
            items.add(TransactionInfoViewItem.ContactItem(it))
        }

        return items
    }

    fun getContractMethodSectionItems(
        method: String?,
        contractAddress: String,
        blockchainType: BlockchainType,
    ) = listOf(
        TransactionInfoViewItem.Transaction(
            method ?: Translator.getString(R.string.Transactions_ContractCall),
            evmLabelManager.mapped(contractAddress),
            TransactionViewItem.Icon.Platform(blockchainType).iconRes
        )
    )

    fun getBitcoinSectionItems(
        transaction: BitcoinTransactionRecord,
        lastBlockInfo: LastBlockInfo?,
    ): List<TransactionInfoViewItem> {
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
            items.add(TransactionInfoViewItem.RawTransaction)
        }

        val lockState = transaction.lockState(lastBlockInfo?.timestamp)
        getLockStateItem(lockState)?.let {
            items.add(it)
        }

        return items
    }

    fun getStatusSectionItems(
        transaction: TransactionRecord,
        status: TransactionStatus,
        rates: Map<String, CurrencyValue?>,
        blockchainType: BlockchainType,
    ): List<TransactionInfoViewItem> {
        val items: MutableList<TransactionInfoViewItem> = mutableListOf(
            TransactionInfoViewItem.Value(
                Translator.getString(R.string.TransactionInfo_Date),
                DateHelper.getFullDate(Date(transaction.timestamp * 1000))
            ),
            TransactionInfoViewItem.Status(status)
        )

        when (transaction) {
            is EvmTransactionRecord -> {
                if (!transaction.foreignTransaction && transaction.fee != null) {
                    items.add(getFeeItem(transaction.fee, rates[transaction.fee.coinUid], status))
                }

                if (transaction is SwapTransactionRecord && transaction.valueOut != null) {
                    val recipientItems = mutableListOf<TransactionInfoViewItem>()

                    val recipient = transaction.recipient
                    if (recipient != null) {
                        val contact = getContact(recipient, blockchainType)

                        recipientItems.add(
                            TransactionInfoViewItem.Address(
                                Translator.getString(R.string.TransactionInfo_RecipientHash),
                                recipient,
                                contact == null,
                                blockchainType,
                                StatSection.AddressRecipient
                            )
                        )

                        contact?.let {
                            recipientItems.add(
                                TransactionInfoViewItem.ContactItem(it)
                            )
                        }
                    }

                    items.addAll(0, recipientItems)
                }
            }

            is TronTransactionRecord -> {
                if (!transaction.foreignTransaction && transaction.fee != null) {
                    items.add(getFeeItem(transaction.fee, rates[transaction.fee.coinUid], status))
                }
            }

            is TonTransactionRecord -> {
                items.add(getFeeItem(transaction.fee, rates[transaction.fee.coinUid], status))
            }

            is BitcoinOutgoingTransactionRecord ->
                transaction.fee?.let { items.add(getFee(it, rates[it.coinUid])) }

            is BinanceChainOutgoingTransactionRecord ->
                items.add(getFee(transaction.fee, rates[transaction.fee.coinUid]))

            is SolanaOutgoingTransactionRecord -> {
                if (transaction.fee != null) {
                    items.add(getFeeItem(transaction.fee, rates[transaction.fee.coinUid], status))
                }
            }
        }

        items.add(TransactionInfoViewItem.TransactionHash(transaction.transactionHash))

        return items
    }

    fun getExplorerSectionItems(explorerData: TransactionInfoModule.ExplorerData): List<TransactionInfoViewItem> =
        listOf(
            TransactionInfoViewItem.Explorer(
                Translator.getString(
                    R.string.TransactionInfo_ButtonViewOnExplorerName,
                    explorerData.title
                ),
                explorerData.url
            )
        )

}