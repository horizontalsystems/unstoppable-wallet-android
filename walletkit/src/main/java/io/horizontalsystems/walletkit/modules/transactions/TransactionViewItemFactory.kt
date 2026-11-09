package io.horizontalsystems.walletkit.modules.transactions

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.adapters.NearTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.XrpTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.StellarTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.TonTransactionRecord
import io.horizontalsystems.walletkit.core.alternativeImageUrl
import io.horizontalsystems.walletkit.core.coinIconUrl
import io.horizontalsystems.walletkit.core.iconPlaceholder
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.managers.BalanceHiddenManager
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.core.shorten
import io.horizontalsystems.walletkit.entities.CurrencyValue
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.ContractCreationTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.monero.MoneroIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.monero.MoneroOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.thorchain.ThorchainOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaSwapTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.zano.ZanoIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.zano.ZanoOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.zcash.ZcashShieldingTransactionRecord
import io.horizontalsystems.walletkit.modules.contacts.ContactsRepository
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.transactionInfo.ColorName
import io.horizontalsystems.walletkit.modules.transactionInfo.ColoredValue
import io.horizontalsystems.walletkit.uiv3.components.TransactionIconState
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Date

class TransactionViewItemFactory(
    private val evmLabelManager: EvmLabelManager,
    private val contactsRepository: ContactsRepository,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val localStorage: ILocalStorage,
) {

    private var showAmount = !balanceHiddenManager.balanceHidden
    private val cache = mutableMapOf<String, Map<Long, TransactionViewItem>>()
    private val roundCoinAmount get() = localStorage.amountRoundingEnabled

    fun updateCache() {
        showAmount = !balanceHiddenManager.balanceHidden
        cache.forEach { (recordUid, map) ->
            map.forEach { (createdAt, viewItem) ->
                cache[recordUid] = mapOf(
                    createdAt to viewItem.copy(
                        showAmount = showAmount,
                    )
                )
            }
        }
    }

    fun convertToViewItemCached(transactionItem: TransactionItem): TransactionViewItem {
        cache[transactionItem.record.uid]?.get(transactionItem.createdAt)?.let {
            return it
        }

        val transactionViewItem = convertToViewItem(transactionItem)
        cache[transactionItem.record.uid] = mapOf(transactionItem.createdAt to transactionViewItem)

        return transactionViewItem
    }

    fun convertToViewItem(item: SwapRecordItem): TransactionViewItem {
        val record = item.record
        val state = swapRecordIconState(item.status)
        val titleIn = recordCoinTitle(record.tokenInCoinCode, record.tokenInCoinUid, item.tokenInAlternativeImageUrl)
        val titleOut = recordCoinTitle(record.tokenOutCoinCode, record.tokenOutCoinUid, item.tokenOutAlternativeImageUrl)
        val amountIn = recordAmount(record.amountIn)
        val amountOut = record.amountOut?.let { recordAmount(it) }
        val recipient = record.recipientAddress?.let { address ->
            val blockchainType = BlockchainType.fromUid(record.tokenOutBlockchainTypeUid)
            Translator.getString(R.string.Transactions_To, mapped(address, blockchainType))
        } ?: "---"

        val type: TransactionViewItem.Type
        val title: TransactionViewItem.Title
        val completedSubtitle: String
        val primaryValue: ColoredValue?
        val secondaryValue: ColoredValue?

        when (item.kind) {
            SwapRecordKind.Swap,
            SwapRecordKind.PrivateSwap -> {
                type = if (item.kind == SwapRecordKind.Swap) TransactionViewItem.Type.Swap else TransactionViewItem.Type.PrivateSwap
                title = TransactionViewItem.Title.Pair(titleIn, titleOut)
                completedSubtitle = Translator.getString(R.string.Transactions_Swap)
                // Bought for someone else, the amount is not the user's: grey, as for an on-chain swap.
                primaryValue = amountOut?.let {
                    ColoredValue("+$it", if (record.customRecipientAddress) ColorName.Grey else ColorName.Remus)
                }
                secondaryValue = ColoredValue("-$amountIn", ColorName.Leah)
            }

            SwapRecordKind.PrivateSend -> {
                type = TransactionViewItem.Type.PrivateSend
                title = titleIn
                completedSubtitle = recipient
                primaryValue = ColoredValue("-$amountIn", ColorName.Leah)
                secondaryValue = item.fiatIn?.let { ColoredValue(getCurrencyString(it), ColorName.Grey) }
            }

            SwapRecordKind.CrossPay -> {
                type = TransactionViewItem.Type.CrossPay
                title = TransactionViewItem.Title.Pair(titleIn, titleOut)
                completedSubtitle = recipient
                // What the recipient was paid, then what it cost.
                primaryValue = ColoredValue("-${amountOut ?: amountIn}", ColorName.Leah)
                secondaryValue = amountOut?.let { ColoredValue(amountIn, ColorName.Grey) }
            }
        }

        val subtitle = when (item.status) {
            SwapStatus.Refunded -> Translator.getString(R.string.SwapInfo_StatusRefunded)
            SwapStatus.Expired -> Translator.getString(R.string.SwapInfo_StatusExpired)
            SwapStatus.ActionRequired -> Translator.getString(R.string.SwapHistory_StatusActionRequired)
            else -> type.stateText(state)?.let { Translator.getString(it) } ?: completedSubtitle
        }

        return TransactionViewItem(
            uid = swapRecordUid(record.id),
            type = type,
            state = state,
            title = title,
            subtitle = subtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            date = Date(record.timestamp),
            showAmount = showAmount,
            swapRecordId = record.id,
        )
    }

    private fun recordCoinTitle(coinCode: String, coinUid: String, alternativeImageUrl: String?) =
        TransactionViewItem.Title.Single(
            coinCode,
            TransactionViewItem.Icon.Regular(coinUid.coinIconUrl, alternativeImageUrl, R.drawable.coin_placeholder)
        )

    private fun recordAmount(amount: String): String {
        val decimal = amount.toBigDecimalOrNull()?.abs() ?: return amount
        return if (roundCoinAmount) {
            App.numberFormatter.formatCoinShort(decimal, null, 8)
        } else {
            App.numberFormatter.formatCoinFull(decimal, null, 8)
        }
    }

    private fun coinIconType(token: Token) = TransactionViewItem.Icon.Regular(
        token.coin.imageUrl,
        token.coin.alternativeImageUrl,
        token.fullCoin.iconPlaceholder
    )

    private fun regularIconType(
        value: TransactionValue?
    ): TransactionViewItem.Icon.Regular =
        when (value) {
            is TransactionValue.NftValue -> {
                TransactionViewItem.Icon.Regular(
                    null,
                    null,
                    R.drawable.icon_24_nft_placeholder,
                    rectangle = true
                )
            }

            is TransactionValue.CoinValue,
            is TransactionValue.RawValue,
            is TransactionValue.JettonValue,
            is TransactionValue.TokenValue -> {
                TransactionViewItem.Icon.Regular(value.coinIconUrl, value.alternativeCoinIconUrl, value.coinIconPlaceholder)
            }

            null -> TransactionViewItem.Icon.Regular(null, null, R.drawable.coin_placeholder)
        }

    // A raw value is a token the wallet could not identify: no code, no icon, no decimals.
    private fun unknownAsset(value: TransactionValue?) = value == null || value is TransactionValue.RawValue

    private fun coinTitle(value: TransactionValue?) = if (unknownAsset(value)) {
        TransactionViewItem.Title.Single(
            text = UNKNOWN,
            icon = TransactionViewItem.Icon.Regular(null, null, R.drawable.coin_placeholder),
            muted = true
        )
    } else {
        TransactionViewItem.Title.Single(value?.coinCode ?: "", regularIconType(value))
    }

    // The amount of one side of a swap; an asset the wallet cannot identify has none to show.
    private fun swapAmount(value: TransactionValue?, color: ColorName) =
        value?.let { getColoredValue(it, color) } ?: ColoredValue(UNKNOWN, color)

    private fun tokenTitle(token: Token) =
        TransactionViewItem.Title.Single(token.coin.code, coinIconType(token))

    private fun pairTitle(from: TransactionValue?, to: TransactionValue?) =
        TransactionViewItem.Title.Pair(coinTitle(from), coinTitle(to))

    private fun namedTitle(text: String, blockchainType: BlockchainType) =
        TransactionViewItem.Title.Single(text, TransactionViewItem.Icon.Platform(blockchainType))

    // Null when the values do not reduce to one coin or one pair: the row is then titled by name.
    private fun coinsTitle(
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>
    ): TransactionViewItem.Title? = when {
        incomingValues.size == 1 && outgoingValues.isEmpty() -> coinTitle(incomingValues[0])
        incomingValues.isEmpty() && outgoingValues.size == 1 -> coinTitle(outgoingValues[0])
        incomingValues.size == 1 && outgoingValues.size == 1 -> pairTitle(outgoingValues[0], incomingValues[0])
        else -> null
    }

    private fun viewItem(
        uid: String,
        type: TransactionViewItem.Type,
        state: TransactionIconState,
        title: TransactionViewItem.Title,
        completedSubtitle: String,
        primaryValue: ColoredValue?,
        secondaryValue: ColoredValue?,
        timestamp: Long,
        spam: Boolean,
        doubleSpend: Boolean = false,
        locked: Boolean? = null,
    ) = TransactionViewItem(
        uid = uid,
        type = type,
        state = state,
        title = title,
        subtitle = type.stateText(state)?.let { Translator.getString(it) } ?: completedSubtitle,
        primaryValue = primaryValue,
        secondaryValue = secondaryValue,
        date = Date(timestamp * 1000),
        showAmount = showAmount,
        doubleSpend = doubleSpend,
        spam = spam,
        locked = locked,
    )

    private fun sendType(sentToSelf: Boolean) =
        if (sentToSelf) TransactionViewItem.Type.SendToSelf else TransactionViewItem.Type.Send

    private fun sendSubtitle(sentToSelf: Boolean, to: String?, blockchainType: BlockchainType, noAddress: String = "---") = when {
        sentToSelf -> Translator.getString(R.string.Transactions_ToYourself)
        to != null -> Translator.getString(R.string.Transactions_To, mapped(to, blockchainType))
        else -> noAddress
    }

    private fun sendPrimaryValue(value: TransactionValue, sentToSelf: Boolean) = if (sentToSelf) {
        ColoredValue(getCoinString(value, true), ColorName.Leah)
    } else {
        getColoredValue(value, ColorName.Leah)
    }

    private fun receiveSubtitle(from: String?, blockchainType: BlockchainType) =
        from?.let { Translator.getString(R.string.Transactions_From, mapped(it, blockchainType)) } ?: Translator.getString(R.string.Transactions_FromUnknown)

    private fun locked(lockState: TransactionLockState?) = when {
        lockState == null -> null
        lockState.locked -> true
        else -> false
    }

    private fun convertToViewItem(transactionItem: TransactionItem): TransactionViewItem {
        val record = transactionItem.record
        val status = record.status(transactionItem.lastBlockInfo?.height)
        val state = transactionIconState(status)
        val currencyValue = transactionItem.currencyValue
        val lastBlockTimestamp = transactionItem.lastBlockInfo?.timestamp

        return when (record) {
            is ApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(
                uid = record.uid,
                value = record.value,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is BitcoinIncomingTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Receive,
                state = state,
                title = coinTitle(record.value),
                completedSubtitle = receiveSubtitle(record.from, record.blockchainType),
                primaryValue = getColoredValue(record.value, ColorName.Remus),
                secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) },
                timestamp = record.timestamp,
                spam = record.spam,
                doubleSpend = record.conflictingHash != null,
                locked = locked(record.lockState(lastBlockTimestamp))
            )

            is BitcoinOutgoingTransactionRecord -> viewItem(
                uid = record.uid,
                type = sendType(record.sentToSelf),
                state = state,
                title = coinTitle(record.value),
                completedSubtitle = sendSubtitle(record.sentToSelf, record.to, record.blockchainType),
                primaryValue = sendPrimaryValue(record.value, record.sentToSelf),
                secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) },
                timestamp = record.timestamp,
                spam = record.spam,
                doubleSpend = record.conflictingHash != null,
                locked = locked(record.lockState(lastBlockTimestamp))
            )

            is ContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(record.incomingEvents, record.outgoingEvents)
                createViewItemFromContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    method = record.method,
                    contractAddress = record.contractAddress,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = currencyValue,
                    state = state,
                    spam = record.spam
                )
            }

            is ExternalContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(record.incomingEvents, record.outgoingEvents)
                createViewItemFromExternalContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    incomingEvents = record.incomingEvents,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = currencyValue,
                    state = state,
                    spam = record.spam
                )
            }

            is ContractCreationTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Contract,
                state = state,
                title = namedTitle(Translator.getString(R.string.Transactions_ContractCreation), record.blockchainType),
                completedSubtitle = "---",
                primaryValue = null,
                secondaryValue = null,
                timestamp = record.timestamp,
                spam = record.spam
            )

            is EvmIncomingTransactionRecord -> createViewItemFromIncoming(
                uid = record.uid,
                value = record.value,
                from = record.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is EvmOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is SwapTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Swap,
                state = state,
                title = pairTitle(record.valueIn, record.valueOut),
                completedSubtitle = Translator.getString(R.string.Transactions_Swap),
                primaryValue = swapAmount(record.valueOut, if (record.recipient != null) ColorName.Grey else ColorName.Remus),
                secondaryValue = getColoredValue(record.valueIn, ColorName.Leah),
                timestamp = record.timestamp,
                spam = record.spam
            )

            is UnknownSwapTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Swap,
                state = state,
                title = pairTitle(record.valueIn, record.valueOut),
                completedSubtitle = Translator.getString(R.string.Transactions_Swap),
                primaryValue = swapAmount(record.valueOut, ColorName.Remus),
                secondaryValue = swapAmount(record.valueIn, ColorName.Leah),
                timestamp = record.timestamp,
                spam = record.spam
            )

            is EvmTransactionRecord -> createViewItemFromUnknownTransaction(
                uid = record.uid,
                name = Translator.getString(R.string.Transactions_Unknown),
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                state = state,
                spam = record.spam
            )

            is SolanaIncomingTransactionRecord -> createViewItemFromIncoming(
                uid = record.uid,
                value = record.value,
                from = record.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is SolanaOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam,
                noAddress = ""
            )

            is SolanaSwapTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Swap,
                state = state,
                // The pair by token, not by value: a 1inch Fusion transaction moves only one side
                // (the other lands in its counterpart transaction) but names both tokens.
                title = TransactionViewItem.Title.Pair(
                    from = record.tokenIn?.let { tokenTitle(it) } ?: coinTitle(record.valueIn),
                    to = record.tokenOut?.let { tokenTitle(it) } ?: coinTitle(record.valueOut)
                ),
                completedSubtitle = Translator.getString(R.string.Transactions_Swap),
                // A known token without a value is the side that lands in the counterpart transaction.
                primaryValue = if (record.tokenOut != null) {
                    record.valueOut?.let { getColoredValue(it, ColorName.Remus) }
                } else {
                    swapAmount(record.valueOut, ColorName.Remus)
                },
                secondaryValue = if (record.tokenIn != null) {
                    record.valueIn?.let { getColoredValue(it, ColorName.Leah) }
                } else {
                    swapAmount(record.valueIn, ColorName.Leah)
                },
                timestamp = record.timestamp,
                spam = record.spam
            )

            is SolanaUnknownTransactionRecord -> {
                val incomingValues = record.incomingTransfers.map { it.value }
                val outgoingValues = record.outgoingTransfers.map { it.value }
                val (primaryValue, secondaryValue) = getValues(incomingValues, outgoingValues, currencyValue)
                viewItem(
                    uid = record.uid,
                    type = TransactionViewItem.Type.Unknown,
                    state = state,
                    title = coinsTitle(incomingValues, outgoingValues)
                        ?: namedTitle(Translator.getString(R.string.Transactions_Unknown), record.blockchainType),
                    completedSubtitle = Translator.getString(R.string.Transactions_Unknown_Description),
                    primaryValue = primaryValue,
                    secondaryValue = secondaryValue,
                    timestamp = record.timestamp,
                    spam = record.spam
                )
            }

            is TronApproveTransactionRecord -> createViewItemFromApproveTransactionRecord(
                uid = record.uid,
                value = record.value,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is TronContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(record.incomingEvents, record.outgoingEvents)
                createViewItemFromContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    method = record.method,
                    contractAddress = record.contractAddress,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = currencyValue,
                    state = state,
                    spam = record.spam
                )
            }

            is TronExternalContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(record.incomingEvents, record.outgoingEvents)
                createViewItemFromExternalContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    incomingEvents = record.incomingEvents,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = currencyValue,
                    state = state,
                    spam = record.spam
                )
            }

            is TronIncomingTransactionRecord -> createViewItemFromIncoming(
                uid = record.uid,
                value = record.value,
                from = record.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is TronOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is TronTransactionRecord -> createViewItemFromUnknownTransaction(
                uid = record.uid,
                name = record.transaction.contractLabel ?: Translator.getString(R.string.Transactions_Unknown),
                blockchainType = BlockchainType.Tron,
                timestamp = record.timestamp,
                state = state,
                spam = record.spam
            )

            // TON and Stellar rows never showed a pending state; only a failure is kept.
            is TonTransactionRecord -> createViewItemFromTonTransactionRecord(
                record = record,
                currencyValue = currencyValue,
                state = withoutPending(state)
            )

            is StellarTransactionRecord -> createViewItemFromStellarTransactionRecord(
                record = record,
                currencyValue = currencyValue,
                state = withoutPending(state)
            )

            is XrpTransactionRecord -> createViewItemFromXrpTransactionRecord(
                record = record,
                currencyValue = currencyValue,
                state = state
            )

            is ZcashShieldingTransactionRecord -> {
                val type: TransactionViewItem.Type
                val completedSubtitle: String
                when (record.direction) {
                    ZcashShieldingTransactionRecord.Direction.Shield -> {
                        type = TransactionViewItem.Type.Shield
                        completedSubtitle = Translator.getString(R.string.Transactions_Shielded)
                    }

                    ZcashShieldingTransactionRecord.Direction.Unshield -> {
                        type = TransactionViewItem.Type.Unshield
                        completedSubtitle = Translator.getString(R.string.Transactions_Unshielded)
                    }

                    ZcashShieldingTransactionRecord.Direction.MigrateToIronwood -> {
                        type = TransactionViewItem.Type.Migrate
                        completedSubtitle = Translator.getString(R.string.Transactions_Migrate_ToIronwood)
                    }
                }
                viewItem(
                    uid = record.uid,
                    type = type,
                    state = state,
                    title = coinTitle(record.value),
                    completedSubtitle = completedSubtitle,
                    primaryValue = ColoredValue(getCoinString(record.value, true), ColorName.Leah),
                    secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) },
                    timestamp = record.timestamp,
                    spam = record.spam,
                    locked = locked(record.lockState(lastBlockTimestamp))
                )
            }

            is MoneroIncomingTransactionRecord -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Receive,
                state = state,
                title = coinTitle(record.value),
                completedSubtitle = receiveSubtitle(record.from, record.blockchainType),
                primaryValue = getColoredValue(record.value, ColorName.Remus),
                secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) },
                timestamp = record.timestamp,
                spam = record.spam
            )

            is MoneroOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is ThorchainIncomingTransactionRecord -> createViewItemFromIncoming(
                uid = record.uid,
                value = record.value,
                from = record.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is ThorchainOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is ZanoIncomingTransactionRecord -> createViewItemFromIncoming(
                uid = record.uid,
                value = record.value,
                from = record.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is ZanoOutgoingTransactionRecord -> createViewItemFromOutgoing(
                uid = record.uid,
                value = record.value,
                to = record.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = record.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            )

            is NearTransactionRecord -> createViewItemFromNearTransactionRecord(
                record = record,
                currencyValue = currencyValue,
                state = state
            )

            else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
        }
    }

    private fun withoutPending(state: TransactionIconState) =
        if (state == TransactionIconState.Failed) state else TransactionIconState.Completed

    private fun createViewItemFromIncoming(
        uid: String,
        value: TransactionValue,
        from: String?,
        blockchainType: BlockchainType,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
        spam: Boolean,
    ) = viewItem(
        uid = uid,
        type = TransactionViewItem.Type.Receive,
        state = state,
        title = coinTitle(value),
        completedSubtitle = receiveSubtitle(from, blockchainType),
        primaryValue = getColoredValue(value, ColorName.Remus),
        secondaryValue = singleValueSecondaryValue(value, currencyValue),
        timestamp = timestamp,
        spam = spam
    )

    private fun createViewItemFromOutgoing(
        uid: String,
        value: TransactionValue,
        to: String?,
        blockchainType: BlockchainType,
        timestamp: Long,
        sentToSelf: Boolean,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
        spam: Boolean,
        noAddress: String = "---"
    ) = viewItem(
        uid = uid,
        type = sendType(sentToSelf),
        state = state,
        title = coinTitle(value),
        completedSubtitle = sendSubtitle(sentToSelf, to, blockchainType, noAddress),
        primaryValue = sendPrimaryValue(value, sentToSelf),
        secondaryValue = singleValueSecondaryValue(value, currencyValue),
        timestamp = timestamp,
        spam = spam
    )

    private fun createViewItemFromUnknownTransaction(
        uid: String,
        name: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        state: TransactionIconState,
        spam: Boolean
    ) = viewItem(
        uid = uid,
        type = TransactionViewItem.Type.Unknown,
        state = state,
        title = namedTitle(name, blockchainType),
        completedSubtitle = Translator.getString(R.string.Transactions_Unknown_Description),
        primaryValue = null,
        secondaryValue = null,
        timestamp = timestamp,
        spam = spam
    )

    private fun createViewItemFromNearTransactionRecord(
        record: NearTransactionRecord,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
    ): TransactionViewItem = when (val recordType = record.type) {
        is NearTransactionRecord.Type.Send -> createViewItemFromOutgoing(
            uid = record.uid,
            value = recordType.value,
            to = recordType.to,
            blockchainType = record.blockchainType,
            timestamp = record.timestamp,
            sentToSelf = recordType.sentToSelf,
            currencyValue = currencyValue,
            state = state,
            spam = record.spam
        ).copy(secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) })

        is NearTransactionRecord.Type.Receive -> createViewItemFromIncoming(
            uid = record.uid,
            value = recordType.value,
            from = recordType.from,
            blockchainType = record.blockchainType,
            timestamp = record.timestamp,
            currencyValue = currencyValue,
            state = state,
            spam = record.spam
        ).copy(secondaryValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) })

        is NearTransactionRecord.Type.ContractCall -> {
            val incomingValues = recordType.incoming.map { it.value }
            val outgoingValues = recordType.outgoing.map { it.value }
            val name = Translator.getString(R.string.Transactions_ContractCall)
            if (incomingValues.size + outgoingValues.size > 1) {
                multiAssetCallViewItem(
                    uid = record.uid,
                    name = name,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    state = state,
                    spam = record.spam
                )
            } else {
                (incomingValues + outgoingValues).singleOrNull().let { value ->
                    val values = value?.let { getValues(incomingValues, outgoingValues, currencyValue) }
                    viewItem(
                        uid = record.uid,
                        type = TransactionViewItem.Type.Contract,
                        state = state,
                        title = value?.let { coinTitle(it) } ?: namedTitle(name, record.blockchainType),
                        completedSubtitle = if (value != null) {
                            name
                        } else {
                            recordType.method?.let { "$it · ${recordType.contractId.shorten()}" } ?: recordType.contractId.shorten()
                        },
                        primaryValue = values?.first,
                        secondaryValue = values?.second,
                        timestamp = record.timestamp,
                        spam = record.spam
                    )
                }
            }
        }
    }

    private fun createViewItemFromXrpTransactionRecord(
        record: XrpTransactionRecord,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
    ): TransactionViewItem {
        val fiatValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) }
        return when (val recordType = record.type) {
            is XrpTransactionRecord.Type.Send -> createViewItemFromOutgoing(
                uid = record.uid,
                value = recordType.value,
                to = recordType.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = recordType.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            ).copy(secondaryValue = fiatValue)

            is XrpTransactionRecord.Type.Receive -> createViewItemFromIncoming(
                uid = record.uid,
                value = recordType.value,
                from = recordType.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            ).copy(secondaryValue = fiatValue)

            // The value is the trust limit (10^15 by default), not an amount moved, so it
            // has no place on the row and no fiat equivalent; the info page shows it.
            is XrpTransactionRecord.Type.TrustSet -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Contract,
                state = state,
                title = coinTitle(recordType.value),
                completedSubtitle = Translator.getString(R.string.Transactions_TrustSet),
                primaryValue = null,
                secondaryValue = null,
                timestamp = record.timestamp,
                spam = record.spam
            )

            is XrpTransactionRecord.Type.Unsupported -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Contract,
                state = state,
                title = namedTitle(Translator.getString(R.string.Transactions_XrpTransaction), record.blockchainType),
                completedSubtitle = recordType.type,
                primaryValue = null,
                secondaryValue = fiatValue,
                timestamp = record.timestamp,
                spam = record.spam
            )
        }
    }

    private fun createViewItemFromStellarTransactionRecord(
        record: StellarTransactionRecord,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
    ): TransactionViewItem {
        val fiatValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) }
        return when (val recordType = record.type) {
            is StellarTransactionRecord.Type.Send -> createViewItemFromOutgoing(
                uid = record.uid,
                value = recordType.value,
                to = recordType.to,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                sentToSelf = recordType.sentToSelf,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            ).copy(secondaryValue = fiatValue)

            is StellarTransactionRecord.Type.Receive -> createViewItemFromIncoming(
                uid = record.uid,
                value = recordType.value,
                from = recordType.from,
                blockchainType = record.blockchainType,
                timestamp = record.timestamp,
                currencyValue = currencyValue,
                state = state,
                spam = record.spam
            ).copy(secondaryValue = fiatValue)

            // The value is the trust limit, not an amount moved; see the XRP TrustSet row.
            is StellarTransactionRecord.Type.ChangeTrust -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Contract,
                state = state,
                title = coinTitle(recordType.value),
                completedSubtitle = Translator.getString(R.string.Transactions_ChangeTrust),
                primaryValue = null,
                secondaryValue = null,
                timestamp = record.timestamp,
                spam = record.spam
            )

            is StellarTransactionRecord.Type.Swap -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Swap,
                state = state,
                title = pairTitle(recordType.valueIn, recordType.valueOut),
                completedSubtitle = Translator.getString(R.string.Transactions_Swap),
                primaryValue = getColoredValue(recordType.valueOut, ColorName.Remus),
                secondaryValue = getColoredValue(recordType.valueIn, ColorName.Leah),
                timestamp = record.timestamp,
                spam = record.spam
            )

            is StellarTransactionRecord.Type.Unsupported -> viewItem(
                uid = record.uid,
                type = TransactionViewItem.Type.Contract,
                state = state,
                title = namedTitle(Translator.getString(R.string.Transactions_StellarTransaction), record.blockchainType),
                completedSubtitle = stellarOperationName(recordType.type),
                primaryValue = null,
                secondaryValue = null,
                timestamp = record.timestamp,
                spam = record.spam
            )
        }
    }

    private fun createViewItemFromTonTransactionRecord(
        record: TonTransactionRecord,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
    ): TransactionViewItem {
        val fiatValue = currencyValue?.let { getColoredValue(it, ColorName.Grey) }
        val action = record.actions.singleOrNull()

        fun item(
            type: TransactionViewItem.Type,
            title: TransactionViewItem.Title,
            completedSubtitle: String,
            primaryValue: ColoredValue?,
            secondaryValue: ColoredValue? = fiatValue,
        ) = viewItem(
            uid = record.uid,
            type = type,
            state = state,
            title = title,
            completedSubtitle = completedSubtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            timestamp = record.timestamp,
            spam = record.spam
        )

        if (action == null) {
            return item(
                type = TransactionViewItem.Type.Contract,
                title = namedTitle(Translator.getString(R.string.Transactions_TonTransaction), record.blockchainType),
                completedSubtitle = Translator.getString(R.string.Transactions_Multiple),
                primaryValue = null
            )
        }

        return when (val actionType = action.type) {
            is TonTransactionRecord.Action.Type.Send -> item(
                type = sendType(actionType.sentToSelf),
                title = coinTitle(actionType.value),
                completedSubtitle = sendSubtitle(actionType.sentToSelf, actionType.to, record.blockchainType),
                primaryValue = sendPrimaryValue(actionType.value, actionType.sentToSelf)
            )

            is TonTransactionRecord.Action.Type.Receive -> item(
                type = TransactionViewItem.Type.Receive,
                title = coinTitle(actionType.value),
                completedSubtitle = receiveSubtitle(actionType.from, record.blockchainType),
                primaryValue = getColoredValue(actionType.value, ColorName.Remus)
            )

            is TonTransactionRecord.Action.Type.Unsupported -> item(
                type = TransactionViewItem.Type.Contract,
                title = namedTitle(Translator.getString(R.string.Transactions_TonTransaction), record.blockchainType),
                completedSubtitle = actionType.type,
                primaryValue = null,
                secondaryValue = null
            )

            is TonTransactionRecord.Action.Type.Burn -> item(
                type = TransactionViewItem.Type.Send,
                title = coinTitle(actionType.value),
                completedSubtitle = Translator.getString(R.string.Transactions_Burn),
                primaryValue = getColoredValue(actionType.value, ColorName.Leah)
            )

            is TonTransactionRecord.Action.Type.ContractCall -> item(
                type = TransactionViewItem.Type.Contract,
                title = namedTitle(Translator.getString(R.string.Transactions_ContractCall), record.blockchainType),
                completedSubtitle = actionType.address.shorten(),
                primaryValue = getColoredValue(actionType.value, ColorName.Leah)
            )

            is TonTransactionRecord.Action.Type.ContractDeploy -> item(
                type = TransactionViewItem.Type.Contract,
                title = namedTitle(Translator.getString(R.string.Transactions_ContractDeploy), record.blockchainType),
                completedSubtitle = actionType.interfaces.joinToString(),
                primaryValue = null
            )

            is TonTransactionRecord.Action.Type.Mint -> item(
                type = TransactionViewItem.Type.Receive,
                title = coinTitle(actionType.value),
                completedSubtitle = Translator.getString(R.string.Transactions_Mint),
                primaryValue = getColoredValue(actionType.value, ColorName.Remus)
            )

            is TonTransactionRecord.Action.Type.Swap -> item(
                type = TransactionViewItem.Type.Swap,
                title = pairTitle(actionType.valueIn, actionType.valueOut),
                completedSubtitle = Translator.getString(R.string.Transactions_Swap),
                primaryValue = getColoredValue(actionType.valueOut, ColorName.Remus),
                secondaryValue = getColoredValue(actionType.valueIn, ColorName.Leah)
            )
        }
    }

    // A call that moves more than one asset is titled by name alone, without coins or amounts.
    private fun multiAssetCallViewItem(
        uid: String,
        name: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        state: TransactionIconState,
        spam: Boolean
    ) = viewItem(
        uid = uid,
        type = TransactionViewItem.Type.Contract,
        state = state,
        title = namedTitle(name, blockchainType),
        completedSubtitle = Translator.getString(R.string.Transactions_Completed),
        primaryValue = null,
        secondaryValue = null,
        timestamp = timestamp,
        spam = spam
    )

    private fun getContact(address: String, blockchainType: BlockchainType): Contact? {
        return contactsRepository.getContactsFiltered(blockchainType, addressQuery = address).firstOrNull()
    }

    private fun mapped(address: String, blockchainType: BlockchainType): String {
        return getContact(address, blockchainType)?.name ?: evmLabelManager.mapped(address)
    }

    private fun getColoredValue(value: Any, color: ColorName, hideSign: Boolean = false): ColoredValue =
        when (value) {
            is TransactionValue -> ColoredValue(
                getCoinString(value, hideSign).ifEmpty { UNKNOWN },
                if (value.zeroValue) ColorName.Leah else color
            )
            is CurrencyValue -> ColoredValue(getCurrencyString(value), if (value.value.compareTo(BigDecimal.ZERO) == 0) ColorName.Grey else color)
            else -> ColoredValue(value.toString(), color)
        }

    private fun createViewItemFromContractCallTransactionRecord(
        uid: String,
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>,
        method: String?,
        contractAddress: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
        spam: Boolean
    ): TransactionViewItem {
        val name = method ?: Translator.getString(R.string.Transactions_ContractCall)
        if (incomingValues.size + outgoingValues.size > 1) {
            return multiAssetCallViewItem(uid, name, blockchainType, timestamp, state, spam)
        }

        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(incomingValues, outgoingValues, currencyValue)
        val coinsTitle = coinsTitle(incomingValues, outgoingValues)

        return viewItem(
            uid = uid,
            type = TransactionViewItem.Type.Contract,
            state = state,
            title = coinsTitle ?: namedTitle(name, blockchainType),
            completedSubtitle = if (coinsTitle != null) name else mapped(contractAddress, blockchainType),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            timestamp = timestamp,
            spam = spam
        )
    }

    private fun createViewItemFromExternalContractCallTransactionRecord(
        uid: String,
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>,
        incomingEvents: List<TransferEvent>,
        blockchainType: BlockchainType,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
        spam: Boolean
    ): TransactionViewItem {
        if (outgoingValues.isNotEmpty() && incomingValues.size + outgoingValues.size > 1) {
            return multiAssetCallViewItem(
                uid = uid,
                name = Translator.getString(R.string.Transactions_ExternalContractCall),
                blockchainType = blockchainType,
                timestamp = timestamp,
                state = state,
                spam = spam
            )
        }

        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(
            incomingValues,
            outgoingValues,
            currencyValue
        )
        val coinsTitle = coinsTitle(incomingValues, outgoingValues)

        val type: TransactionViewItem.Type
        val title: TransactionViewItem.Title
        val subtitle: String
        if (outgoingValues.isEmpty()) {
            type = TransactionViewItem.Type.Receive
            title = coinsTitle ?: namedTitle(Translator.getString(R.string.Transactions_Receive), blockchainType)
            val addresses = incomingEvents.mapNotNull { it.address }.toSet().toList()
            // Several senders name none of them.
            subtitle = receiveSubtitle(addresses.singleOrNull(), blockchainType)
        } else {
            val name = Translator.getString(R.string.Transactions_ExternalContractCall)
            type = TransactionViewItem.Type.Contract
            title = coinsTitle ?: namedTitle(name, blockchainType)
            subtitle = if (coinsTitle != null) name else "---"
        }

        return viewItem(
            uid = uid,
            type = type,
            state = state,
            title = title,
            completedSubtitle = subtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            timestamp = timestamp,
            spam = spam
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        uid: String,
        value: TransactionValue,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        state: TransactionIconState,
        spam: Boolean
    ): TransactionViewItem {
        val primaryValueText: String
        val secondaryValueText: String?

        if (value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText = Translator.getString(R.string.Swap_Approve_Unlimited)
        } else {
            primaryValueText = getCoinString(value, hideSign = true)
            secondaryValueText = currencyValue?.let { getCurrencyString(it) }
        }

        return viewItem(
            uid = uid,
            type = TransactionViewItem.Type.Approve,
            state = state,
            title = coinTitle(value),
            completedSubtitle = Translator.getString(R.string.Transactions_Approve),
            primaryValue = ColoredValue(primaryValueText, ColorName.Leah),
            secondaryValue = secondaryValueText?.let { ColoredValue(it, ColorName.Grey) },
            timestamp = timestamp,
            spam = spam
        )
    }

    private fun singleValueSecondaryValue(
        value: TransactionValue,
        currencyValue: CurrencyValue?
    ): ColoredValue? =
        when (value) {
            is TransactionValue.NftValue -> {
                val text = value.tokenName?.let { "$it #${value.nftUid.tokenId}" } ?: "#${value.nftUid.tokenId}"
                getColoredValue(text, ColorName.Grey)
            }

            is TransactionValue.CoinValue,
            is TransactionValue.RawValue,
            is TransactionValue.JettonValue,
            is TransactionValue.TokenValue -> {
                currencyValue?.let { getColoredValue(it, ColorName.Grey) }
            }
        }

    private fun getValues(
        incomingValues: List<TransactionValue>,
        outgoingValues: List<TransactionValue>,
        currencyValue: CurrencyValue?
    ): Pair<ColoredValue, ColoredValue?> {
        val primaryValue: ColoredValue?
        val secondaryValue: ColoredValue?

        when {
            // incoming
            (incomingValues.size == 1 && outgoingValues.isEmpty()) -> {
                val transactionValue = incomingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Remus)
                secondaryValue = singleValueSecondaryValue(transactionValue, currencyValue)
            }

            // outgoing
            (incomingValues.isEmpty() && outgoingValues.size == 1) -> {
                val transactionValue = outgoingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Leah)
                secondaryValue = singleValueSecondaryValue(transactionValue, currencyValue)
            }

            // swap
            (incomingValues.size == 1 && outgoingValues.size == 1) -> {
                val inTransactionValue = incomingValues.first()
                val outTransactionValue = outgoingValues.first()
                primaryValue = getColoredValue(inTransactionValue, ColorName.Remus)
                secondaryValue = getColoredValue(outTransactionValue, ColorName.Leah)
            }

            // outgoing multiple
            (incomingValues.isEmpty() && outgoingValues.isNotEmpty()) -> {
                primaryValue = getColoredValue(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Leah)
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
                    getColoredValue(outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "), ColorName.Leah)
                }
            }
        }
        return Pair(primaryValue, secondaryValue)
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiatShort(currencyValue.value.abs(), currencyValue.currency.symbol, 2)
    }

    private fun getCoinString(transactionValue: TransactionValue, hideSign: Boolean = false): String {
        return transactionValue.decimalValue?.let { decimalValue ->
            val sign = when {
                hideSign -> ""
                decimalValue < BigDecimal.ZERO -> "-"
                decimalValue > BigDecimal.ZERO -> "+"
                else -> ""
            }
            val coinAmount = if (roundCoinAmount) {
                App.numberFormatter.formatCoinShort(
                    decimalValue.abs(),
                    null,
                    transactionValue.decimals ?: 8,
                )
            } else {
                App.numberFormatter.formatCoinFull(
                    decimalValue.abs(),
                    null,
                    transactionValue.decimals ?: 8,
                )
            }
            sign + coinAmount
        } ?: ""
    }

}

private const val UNKNOWN = "---"

// Horizon names operations in snake case: create_claimable_balance reads Create claimable balance.
internal fun stellarOperationName(type: String) =
    type.replace('_', ' ').replaceFirstChar { it.uppercase() }

// Short ids keep the row's test tag, which takes the first 8 characters, unique.
internal fun swapRecordUid(id: Int) = "sr$id"

internal fun transactionIconState(status: TransactionStatus) = when (status) {
    TransactionStatus.Pending, is TransactionStatus.Processing -> TransactionIconState.Pending
    TransactionStatus.Failed -> TransactionIconState.Failed
    TransactionStatus.Completed -> TransactionIconState.Completed
}
