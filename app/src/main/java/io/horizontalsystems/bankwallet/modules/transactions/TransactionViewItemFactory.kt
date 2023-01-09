package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.nft.NftAssetBriefMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColorName
import io.horizontalsystems.bankwallet.modules.transactionInfo.ColoredValue
import java.math.BigDecimal
import java.util.*

class TransactionViewItemFactory(
    private val evmLabelManager: EvmLabelManager
) {

    private val cache = mutableMapOf<String, Map<Long, TransactionViewItem>>()

    fun convertToViewItemCached(transactionItem: TransactionItem): TransactionViewItem {
        cache[transactionItem.record.uid]?.get(transactionItem.createdAt)?.let {
            return it
        }

        val transactionViewItem = convertToViewItem(transactionItem)
        cache[transactionItem.record.uid] = mapOf(transactionItem.createdAt to transactionViewItem)

        return transactionViewItem
    }

    private fun singleValueIconType(
        value: TransactionValue,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf()
    ): TransactionViewItem.Icon =
        when (value) {
            is TransactionValue.NftValue -> {
                TransactionViewItem.Icon.Regular(nftMetadata[value.nftUid]?.previewImageUrl, R.drawable.icon_24_nft_placeholder, rectangle = true)
            }
            is TransactionValue.CoinValue,
            is TransactionValue.RawValue,
            is TransactionValue.TokenValue -> {
                TransactionViewItem.Icon.Regular(value.coinIconUrl, value.coinIconPlaceholder)
            }
        }

    private fun doubleValueIconType(
        primaryValue: TransactionValue?,
        secondaryValue: TransactionValue?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf()
    ): TransactionViewItem.Icon {
        var backUrl: String? = null
        var backPlaceHolder: Int? = null
        var backRectangle = false
        var frontUrl: String? = null
        var frontPlaceHolder: Int? = null
        var frontRectangle = false

        if (primaryValue != null) {
            when (primaryValue) {
                is TransactionValue.NftValue -> {
                    frontRectangle = true
                    frontUrl = nftMetadata[primaryValue.nftUid]?.previewImageUrl
                    frontPlaceHolder = R.drawable.icon_24_nft_placeholder
                }
                is TransactionValue.CoinValue,
                is TransactionValue.RawValue,
                is TransactionValue.TokenValue -> {
                    frontRectangle = false
                    frontUrl = primaryValue.coinIconUrl
                    frontPlaceHolder = primaryValue.coinIconPlaceholder
                }
            }
        } else {
            frontRectangle = false
            frontUrl = null
            frontPlaceHolder = R.drawable.coin_placeholder
        }

        if (secondaryValue != null) {
            when (secondaryValue) {
                is TransactionValue.NftValue -> {
                    backRectangle = true
                    backUrl = nftMetadata[secondaryValue.nftUid]?.previewImageUrl
                    backPlaceHolder = R.drawable.icon_24_nft_placeholder
                }
                is TransactionValue.CoinValue,
                is TransactionValue.RawValue,
                is TransactionValue.TokenValue -> {
                    backRectangle = false
                    backUrl = secondaryValue.coinIconUrl
                    backPlaceHolder = secondaryValue.coinIconPlaceholder
                }
            }
        } else {
            backRectangle = false
            backUrl = null
            backPlaceHolder = R.drawable.coin_placeholder
        }

        return TransactionViewItem.Icon.Double(
            back = TransactionViewItem.Icon.Regular(backUrl, backPlaceHolder, backRectangle),
            front = TransactionViewItem.Icon.Regular(frontUrl, frontPlaceHolder, frontRectangle)
        )
    }

    private fun iconType(
        source: TransactionSource,
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem.Icon = when {
        incomingValues.size == 1 && outgoingValues.isEmpty() -> {
            singleValueIconType(incomingValues[0], nftMetadata)
        }
        incomingValues.isEmpty() && outgoingValues.size == 1 -> {
            singleValueIconType(outgoingValues[0], nftMetadata)
        }
        incomingValues.size == 1 && outgoingValues.size == 1 -> {
            doubleValueIconType(incomingValues[0], outgoingValues[0], nftMetadata)
        }
        else -> {
            TransactionViewItem.Icon.Platform(source)
        }
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
            is ApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(record, transactionItem.currencyValue, progress, icon)
            is BinanceChainIncomingTransactionRecord -> createViewItemFromBinanceChainIncomingTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                icon
            )
            is BinanceChainOutgoingTransactionRecord -> createViewItemFromBinanceChainOutgoingTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                icon
            )
            is BitcoinIncomingTransactionRecord -> createViewItemFromBitcoinIncomingTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                lastBlockTimestamp,
                icon
            )
            is BitcoinOutgoingTransactionRecord -> createViewItemFromBitcoinOutgoingTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                lastBlockTimestamp,
                icon
            )
            is ContractCallTransactionRecord -> createViewItemFromContractCallTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                icon,
                transactionItem.nftMetadata
            )
            is ExternalContractCallTransactionRecord -> createViewItemFromExternalContractCallTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                icon,
                transactionItem.nftMetadata
            )
            is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(record, progress, icon)
            is EvmIncomingTransactionRecord -> createViewItemFromEvmIncomingTransactionRecord(record, transactionItem.currencyValue, progress, icon)
            is EvmOutgoingTransactionRecord -> createViewItemFromEvmOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, icon, transactionItem.nftMetadata)
            is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(record, progress, icon)
            is UnknownSwapTransactionRecord -> createViewItemFromUnknownSwapTransactionRecord(record, progress, icon)
            is EvmTransactionRecord -> createViewItemFromEvmTransactionRecord(record, progress, icon)
            is SolanaIncomingTransactionRecord -> createViewItemFromSolanaIncomingTransactionRecord(record, transactionItem.currencyValue, progress, icon, transactionItem.nftMetadata)
            is SolanaOutgoingTransactionRecord -> createViewItemFromSolanaOutgoingTransactionRecord(record, transactionItem.currencyValue, progress, icon, transactionItem.nftMetadata)
            is SolanaUnknownTransactionRecord -> createViewItemFromSolanaUnknownTransactionRecord(record, transactionItem.currencyValue, progress, icon)
            else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
        }
    }

    private fun createViewItemFromSolanaUnknownTransactionRecord(record: SolanaUnknownTransactionRecord, currencyValue: CurrencyValue?, progress: Float?, icon: TransactionViewItem.Icon.Failed?): TransactionViewItem {
        val incomingValues = record.incomingTransfers.map { it.value }
        val outgoingValues = record.outgoingTransfers.map { it.value }
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(incomingValues, outgoingValues, currencyValue, mutableMapOf())

        return TransactionViewItem(
                uid = record.uid,
                progress = progress,
                title = Translator.getString(R.string.Transactions_Unknown),
                subtitle = Translator.getString(R.string.Transactions_Unknown_Description),
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                date = Date(record.timestamp * 1000),
                icon = icon ?: iconType(record.source, incomingValues, outgoingValues, mutableMapOf())
        )
    }

    private fun createViewItemFromSolanaOutgoingTransactionRecord(record: SolanaOutgoingTransactionRecord, currencyValue: CurrencyValue?, progress: Float?, icon: TransactionViewItem.Icon.Failed?, nftMetadata: Map<NftUid, NftAssetBriefMetadata>): TransactionViewItem {
        val primaryValue = if (record.sentToSelf) {
            ColoredValue(getCoinString(record.value, true), ColorName.Leah)
        } else {
            getColoredValue(record.value, ColorName.Lucian)
        }
        val secondaryValue = singleValueSecondaryValue(record.value, currencyValue, nftMetadata)

        return TransactionViewItem(
                uid = record.uid,
                progress = progress,
                title = Translator.getString(R.string.Transactions_Send),
                subtitle = record.to?.let { Translator.getString(R.string.Transactions_To, it.shorten()) } ?: "",
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                date = Date(record.timestamp * 1000),
                sentToSelf = record.sentToSelf,
                icon = icon ?: singleValueIconType(record.value, nftMetadata)
        )
    }

    private fun createViewItemFromSolanaIncomingTransactionRecord(record: SolanaIncomingTransactionRecord, currencyValue: CurrencyValue?, progress: Float?, icon: TransactionViewItem.Icon.Failed?, nftMetadata: Map<NftUid, NftAssetBriefMetadata>): TransactionViewItem {
        val primaryValue = getColoredValue(record.value, ColorName.Remus)
        val secondaryValue = singleValueSecondaryValue(record.value, currencyValue, nftMetadata)

        return TransactionViewItem(
                uid = record.uid,
                progress = progress,
                title = Translator.getString(R.string.Transactions_Receive),
                subtitle = record.from?.let { Translator.getString(R.string.Transactions_From, it.shorten()) } ?: "",
                primaryValue = primaryValue,
                secondaryValue = secondaryValue,
                date = Date(record.timestamp * 1000),
                icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromSwapTransactionRecord(
        record: SwapTransactionRecord,
        progress: Float?,
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
            icon = icon ?: doubleValueIconType(record.valueOut, record.valueIn)
        )
    }

    private fun createViewItemFromUnknownSwapTransactionRecord(
        record: UnknownSwapTransactionRecord,
        progress: Float?,
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
            icon = icon ?: doubleValueIconType(record.valueOut, record.valueIn)
        )
    }

    private fun createViewItemFromEvmTransactionRecord(
        record: EvmTransactionRecord,
        progress: Float?,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
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
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val primaryValue = if (record.sentToSelf) {
            ColoredValue(getCoinString(record.value, true), ColorName.Leah)
        } else {
            getColoredValue(record.value, ColorName.Lucian)
        }

        val secondaryValue = singleValueSecondaryValue(record.value, currencyValue, nftMetadata)

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Send),
            subtitle = Translator.getString(R.string.Transactions_To, evmLabelManager.mapped(record.to)),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            icon = icon ?: singleValueIconType(record.value, nftMetadata)
        )
    }

    private fun getColoredValue(value: Any, color: ColorName): ColoredValue =
        when (value) {
            is TransactionValue -> ColoredValue(getCoinString(value), if (value.zeroValue) ColorName.Leah else color)
            is CurrencyValue -> ColoredValue(getCurrencyString(value), if (value.value.compareTo(BigDecimal.ZERO) == 0) ColorName.Grey else color)
            else -> ColoredValue(value.toString(), color)
        }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        record: EvmIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
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
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromContractCreationTransactionRecord(
        record: ContractCreationTransactionRecord,
        progress: Float?,
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
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val (incomingValues, outgoingValues) = record.combined(record.incomingEvents, record.outgoingEvents)
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(incomingValues, outgoingValues, currencyValue, nftMetadata)
        val title = record.method ?: Translator.getString(R.string.Transactions_ContractCall)

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = title,
            subtitle = evmLabelManager.mapped(record.contractAddress),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp * 1000),
            icon = icon ?: iconType(record.source, incomingValues, outgoingValues, nftMetadata)
        )
    }

    private fun createViewItemFromExternalContractCallTransactionRecord(
        record: ExternalContractCallTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val (incomingValues, outgoingValues) = record.combined(record.incomingEvents, record.outgoingEvents)

        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(
            incomingValues,
            outgoingValues,
            currencyValue,
            nftMetadata
        )

        val title: String
        val subTitle: String
        if (outgoingValues.isEmpty()) {
            title = Translator.getString(R.string.Transactions_Receive)
            val addresses = record.incomingEvents.mapNotNull { it.address }.toSet().toList()

            subTitle = if (addresses.size == 1) {
                Translator.getString(
                    R.string.Transactions_From, evmLabelManager.mapped(addresses.first())
                )
            } else {
                Translator.getString(R.string.Transactions_Multiple)
            }
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
            icon = icon ?: iconType(record.source, incomingValues, outgoingValues, nftMetadata)
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
            icon = icon ?: singleValueIconType(record.value)
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
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromBinanceChainOutgoingTransactionRecord(
        record: BinanceChainOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
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
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromBinanceChainIncomingTransactionRecord(
        record: BinanceChainIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
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
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        record: ApproveTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
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
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun singleValueSecondaryValue(
        value: TransactionValue,
        currencyValue: CurrencyValue?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): ColoredValue? =
        when (value) {
            is TransactionValue.NftValue -> {
                val text = nftMetadata[value.nftUid]?.name ?: value.tokenName?.let { "$it #${value.nftUid.tokenId}" } ?: "#${value.nftUid.tokenId}"
                getColoredValue(text, ColorName.Grey)
            }
            is TransactionValue.CoinValue,
            is TransactionValue.RawValue,
            is TransactionValue.TokenValue -> {
                currencyValue?.let { getColoredValue(it, ColorName.Grey) }
            }
        }

    private fun getValues(
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>,
        currencyValue: CurrencyValue?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): Pair<ColoredValue, ColoredValue?> {
        val primaryValue: ColoredValue?
        val secondaryValue: ColoredValue?

        when {
            // incoming
            (incomingValues.size == 1 && outgoingValues.isEmpty()) -> {
                val transactionValue = incomingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Remus)
                secondaryValue = singleValueSecondaryValue(transactionValue, currencyValue, nftMetadata)
            }

            // outgoing
            (incomingValues.isEmpty() && outgoingValues.size == 1) -> {
                val transactionValue = outgoingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Lucian)
                secondaryValue = singleValueSecondaryValue(transactionValue, currencyValue, nftMetadata)
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
                primaryValue = if (incomingValues.size == 1) {
                    getColoredValue(incomingValues.first(), ColorName.Remus)
                } else {
                    getColoredValue(incomingValues.joinToString(", ") { it.coinCode }, ColorName.Remus)
                }
                secondaryValue = if (outgoingValues.size == 1) {
                    getColoredValue(outgoingValues.first(), ColorName.Remus)
                } else {
                    getColoredValue(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Lucian)
                }
            }
        }
        return Pair(primaryValue, secondaryValue)
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