package cash.p.terminal.modules.transactions

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.TonTransactionRecord
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.managers.BalanceHiddenManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.storage.ChangeNowTransactionsStorage
import cash.p.terminal.entities.ChangeNowTransaction
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ApproveTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ContractCreationTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.SwapTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronApproveTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.network.changenow.api.ChangeNowHelper
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.strings.helpers.shorten
import cash.p.terminal.ui_compose.ColorName
import cash.p.terminal.ui_compose.ColoredValue
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.useCases.WalletUseCase
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.tronkit.models.Contract
import java.math.BigDecimal
import java.util.Date

class TransactionViewItemFactory(
    private val evmLabelManager: EvmLabelManager,
    private val contactsRepository: ContactsRepository,
    private val balanceHiddenManager: BalanceHiddenManager,
    private val changeNowTransactionsStorage: ChangeNowTransactionsStorage,
    private val walletUseCase: WalletUseCase,
    private val numberFormatter: IAppNumberFormatter
) {

    private var showAmount = !balanceHiddenManager.balanceHidden
    private val cache = mutableMapOf<String, Map<Long, TransactionViewItem>>()

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

    private fun singleValueIconType(
        value: TransactionValue,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf()
    ): TransactionViewItem.Icon =
        when (value) {
            is TransactionValue.NftValue -> {
                TransactionViewItem.Icon.Regular(
                    nftMetadata[value.nftUid]?.previewImageUrl,
                    null,
                    R.drawable.icon_24_nft_placeholder,
                    rectangle = true
                )
            }

            is TransactionValue.CoinValue,
            is TransactionValue.RawValue,
            is TransactionValue.JettonValue,
            is TransactionValue.TokenValue -> {
                TransactionViewItem.Icon.Regular(
                    value.coinIconUrl,
                    value.alternativeCoinIconUrl,
                    value.coinIconPlaceholder
                )
            }
        }

    private fun doubleValueIconType(
        primaryValue: TransactionValue?,
        secondaryValue: TransactionValue?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata> = mapOf()
    ): TransactionViewItem.Icon {
        var backUrl: String? = null
        var backAlternativeUrl: String? = null
        var backPlaceHolder: Int? = null
        var backRectangle = false
        var frontUrl: String? = null
        var frontAlternativeUrl: String? = null
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
                is TransactionValue.JettonValue,
                is TransactionValue.TokenValue -> {
                    frontRectangle = false
                    frontUrl = primaryValue.coinIconUrl
                    frontAlternativeUrl = primaryValue.alternativeCoinIconUrl
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
                    backAlternativeUrl = secondaryValue.alternativeCoinIconUrl
                    backPlaceHolder = secondaryValue.coinIconPlaceholder
                }

                is TransactionValue.JettonValue -> TODO()
            }
        } else {
            backRectangle = false
            backUrl = null
            backPlaceHolder = R.drawable.coin_placeholder
        }

        return TransactionViewItem.Icon.Double(
            back = TransactionViewItem.Icon.Regular(
                backUrl,
                backAlternativeUrl,
                backPlaceHolder,
                backRectangle
            ),
            front = TransactionViewItem.Icon.Regular(
                frontUrl,
                frontAlternativeUrl,
                frontPlaceHolder,
                frontRectangle
            )
        )
    }

    private fun getIconForToken(
        coinUid: String,
        blockchainType: String
    ): TransactionViewItem.Icon.Regular =
        walletUseCase.getWallet(coinUid, blockchainType)?.let { wallet ->
            return TransactionViewItem.Icon.Regular(
                url = wallet.token.coin.imageUrl,
                alternativeUrl = null,
                placeholder = null
            )
        } ?: TransactionViewItem.Icon.Regular(
            url = null,
            alternativeUrl = null,
            placeholder = R.drawable.coin_placeholder
        )

    private fun iconType(
        blockchainType: BlockchainType,
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
            TransactionViewItem.Icon.Platform(blockchainType)
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
            is ApproveTransactionRecord -> {
                createViewItemFromApproveTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    spender = record.spender,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is BinanceChainIncomingTransactionRecord ->
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.value.token,
                    isIncoming = true
                ) ?: createViewItemFromBinanceChainIncomingTransactionRecord(
                    record,
                    transactionItem.currencyValue,
                    progress,
                    icon
                )

            is BinanceChainOutgoingTransactionRecord ->
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.value.token,
                    isIncoming = false
                ) ?: createViewItemFromBinanceChainOutgoingTransactionRecord(
                    record,
                    transactionItem.currencyValue,
                    progress,
                    icon
                )

            is BitcoinIncomingTransactionRecord ->
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.token,
                    isIncoming = true
                ) ?: createViewItemFromBitcoinIncomingTransactionRecord(
                    record,
                    transactionItem.currencyValue,
                    progress,
                    lastBlockTimestamp,
                    icon
                )

            is BitcoinOutgoingTransactionRecord -> tryConvertToChangeNowViewItemSwap(
                transactionItem = transactionItem,
                token = record.token,
                isIncoming = false
            ) ?: createViewItemFromBitcoinOutgoingTransactionRecord(
                record,
                transactionItem.currencyValue,
                progress,
                lastBlockTimestamp,
                icon
            )

            is ContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(
                    record.incomingEvents,
                    record.outgoingEvents
                )
                createViewItemFromContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    method = record.method,
                    contractAddress = record.contractAddress,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    spam = record.spam,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is ExternalContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(
                    record.incomingEvents,
                    record.outgoingEvents
                )
                createViewItemFromExternalContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    incomingEvents = record.incomingEvents,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is ContractCreationTransactionRecord -> createViewItemFromContractCreationTransactionRecord(
                record,
                progress,
                icon
            )

            is EvmIncomingTransactionRecord -> {
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.baseToken,
                    isIncoming = true
                ) ?: createViewItemFromEvmIncomingTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    from = record.from,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is EvmOutgoingTransactionRecord -> {
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = (record.mainValue as? TransactionValue.CoinValue)?.token,
                    isIncoming = false
                ) ?: createViewItemFromEvmOutgoingTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    to = record.to,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    sentToSelf = record.sentToSelf,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is SwapTransactionRecord -> createViewItemFromSwapTransactionRecord(
                record,
                progress,
                icon
            )

            is UnknownSwapTransactionRecord -> createViewItemFromUnknownSwapTransactionRecord(
                record,
                progress,
                icon
            )

            is EvmTransactionRecord -> {
                createViewItemFromEvmTransactionRecord(
                    uid = record.uid,
                    timestamp = record.timestamp,
                    blockchainType = record.blockchainType,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is SolanaIncomingTransactionRecord ->
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.baseToken,
                    isIncoming = true
                ) ?: createViewItemFromSolanaIncomingTransactionRecord(
                    record = record,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    nftMetadata = transactionItem.nftMetadata
                )

            is SolanaOutgoingTransactionRecord ->
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.baseToken,
                    isIncoming = false
                ) ?: createViewItemFromSolanaOutgoingTransactionRecord(
                    record = record,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    nftMetadata = transactionItem.nftMetadata
                )

            is SolanaUnknownTransactionRecord -> createViewItemFromSolanaUnknownTransactionRecord(
                record = record,
                currencyValue = transactionItem.currencyValue,
                progress = progress,
                icon = icon
            )

            is TronApproveTransactionRecord -> {
                createViewItemFromApproveTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    spender = record.spender,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is TronContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(
                    record.incomingEvents,
                    record.outgoingEvents
                )
                createViewItemFromContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    method = record.method,
                    contractAddress = record.contractAddress,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    spam = record.spam,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is TronExternalContractCallTransactionRecord -> {
                val (incomingValues, outgoingValues) = EvmTransactionRecord.combined(
                    record.incomingEvents,
                    record.outgoingEvents
                )
                createViewItemFromExternalContractCallTransactionRecord(
                    uid = record.uid,
                    incomingValues = incomingValues,
                    outgoingValues = outgoingValues,
                    incomingEvents = record.incomingEvents,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    spam = record.spam,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is TronIncomingTransactionRecord -> {
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.baseToken,
                    isIncoming = true
                ) ?: createViewItemFromEvmIncomingTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    from = record.from,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is TronOutgoingTransactionRecord -> {
                tryConvertToChangeNowViewItemSwap(
                    transactionItem = transactionItem,
                    token = record.baseToken,
                    isIncoming = false
                ) ?: createViewItemFromEvmOutgoingTransactionRecord(
                    uid = record.uid,
                    value = record.value,
                    to = record.to,
                    blockchainType = record.blockchainType,
                    timestamp = record.timestamp,
                    sentToSelf = record.sentToSelf,
                    currencyValue = transactionItem.currencyValue,
                    progress = progress,
                    icon = icon,
                    spam = record.spam,
                    nftMetadata = transactionItem.nftMetadata
                )
            }

            is TronTransactionRecord -> {
                createViewItemFromTronTransactionRecord(
                    uid = record.uid,
                    timestamp = record.timestamp,
                    contract = record.transaction.contract,
                    progress = progress,
                    spam = record.spam,
                    icon = icon
                )
            }

            is TonTransactionRecord -> {
                createViewItemFromTonTransactionRecord(
                    icon = icon,
                    record = record,
                    currencyValue = transactionItem.currencyValue
                )
            }

            else -> throw IllegalArgumentException("Undefined record type ${record.javaClass.name}")
        }
    }

    private fun createViewItemFromTonTransactionRecord(
        icon: TransactionViewItem.Icon?,
        record: TonTransactionRecord,
        currencyValue: CurrencyValue?,
    ): TransactionViewItem {
        val title: String
        val subtitle: String?
        val primaryValue: ColoredValue?
        var secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        val iconX: TransactionViewItem.Icon
        var sentToSelf = false

        val action = record.actions.singleOrNull()

        if (action != null) {
            when (val actionType = action.type) {
                is TonTransactionRecord.Action.Type.Send -> {
                    title = Translator.getString(R.string.Transactions_Send)
                    subtitle = Translator.getString(
                        R.string.Transactions_To,
                        mapped(actionType.to, record.blockchainType)
                    )

                    primaryValue = getColoredValue(actionType.value, ColorName.Lucian)

                    sentToSelf = actionType.sentToSelf

                    iconX = singleValueIconType(actionType.value)
                }

                is TonTransactionRecord.Action.Type.Receive -> {
                    title = Translator.getString(R.string.Transactions_Receive)
                    subtitle = Translator.getString(
                        R.string.Transactions_From,
                        mapped(actionType.from, record.blockchainType)
                    )

                    primaryValue = getColoredValue(actionType.value, ColorName.Remus)
                    iconX = singleValueIconType(actionType.value)
                }

                is TonTransactionRecord.Action.Type.Unsupported -> {
                    title = Translator.getString(R.string.Transactions_TonTransaction)
                    subtitle = actionType.type
                    primaryValue = null
                    secondaryValue = null


                    iconX = TransactionViewItem.Icon.Platform(record.blockchainType)
                }

                is TonTransactionRecord.Action.Type.Burn -> {
                    iconX = singleValueIconType(actionType.value)
                    title = Translator.getString(R.string.Transactions_Burn)
                    subtitle = actionType.value.fullName
                    primaryValue = getColoredValue(actionType.value, ColorName.Lucian)
                }

                is TonTransactionRecord.Action.Type.ContractCall -> {
                    iconX = TransactionViewItem.Icon.Platform(record.blockchainType)
                    title = Translator.getString(R.string.Transactions_ContractCall)
                    subtitle = actionType.address.shorten()
                    primaryValue = getColoredValue(actionType.value, ColorName.Lucian)
                }

                is TonTransactionRecord.Action.Type.ContractDeploy -> {
                    iconX = TransactionViewItem.Icon.Platform(record.blockchainType)
                    title = Translator.getString(R.string.Transactions_ContractDeploy)
                    subtitle = actionType.interfaces.joinToString()
                    primaryValue = null
                }

                is TonTransactionRecord.Action.Type.Mint -> {
                    iconX = singleValueIconType(actionType.value)
                    title = Translator.getString(R.string.Transactions_Mint)
                    subtitle = actionType.value.fullName
                    primaryValue = getColoredValue(actionType.value, ColorName.Remus)
                }

                is TonTransactionRecord.Action.Type.Swap -> {
                    iconX = doubleValueIconType(actionType.valueOut, actionType.valueIn)
                    title = Translator.getString(R.string.Transactions_Swap)
                    subtitle = actionType.routerName ?: actionType.routerAddress.shorten()
                    primaryValue = getColoredValue(actionType.valueOut, ColorName.Remus)
                    secondaryValue = getColoredValue(actionType.valueIn, ColorName.Lucian)
                }
            }
        } else {
            iconX = TransactionViewItem.Icon.Platform(record.blockchainType)
            title = Translator.getString(R.string.Transactions_TonTransaction)
            subtitle = Translator.getString(R.string.Transactions_Multiple)
            primaryValue = null
        }

        return TransactionViewItem(
            uid = record.uid,
            progress = null,
            title = title,
            subtitle = subtitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            sentToSelf = sentToSelf,
            date = Date(record.timestamp * 1000),
            icon = icon ?: iconX
        )
    }

    private fun createViewItemFromSolanaUnknownTransactionRecord(
        record: SolanaUnknownTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        icon: TransactionViewItem.Icon.Failed?
    ): TransactionViewItem {
        val incomingValues = record.incomingTransfers.map { it.value }
        val outgoingValues = record.outgoingTransfers.map { it.value }
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(
            incomingValues,
            outgoingValues,
            currencyValue,
            mutableMapOf()
        )

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Unknown),
            subtitle = Translator.getString(R.string.Transactions_Unknown_Description),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            spam = record.spam,
            icon = icon ?: iconType(
                record.blockchainType,
                incomingValues,
                outgoingValues,
                mutableMapOf()
            )
        )
    }

    private fun createViewItemFromSolanaOutgoingTransactionRecord(
        record: SolanaOutgoingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        icon: TransactionViewItem.Icon.Failed?,
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
            subtitle = record.to?.let { to ->
                Translator.getString(
                    R.string.Transactions_To,
                    mapped(to, record.blockchainType)
                )
            } ?: "",
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            spam = record.spam,
            icon = icon ?: singleValueIconType(record.value, nftMetadata)
        )
    }

    private fun createViewItemFromSolanaIncomingTransactionRecord(
        record: SolanaIncomingTransactionRecord,
        currencyValue: CurrencyValue?,
        progress: Float?,
        icon: TransactionViewItem.Icon.Failed?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val primaryValue = getColoredValue(record.value, ColorName.Remus)
        val secondaryValue = singleValueSecondaryValue(record.value, currencyValue, nftMetadata)

        return TransactionViewItem(
            uid = record.uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Receive),
            subtitle = record.from?.let { from ->
                Translator.getString(
                    R.string.Transactions_From,
                    mapped(from, record.blockchainType)
                )
            } ?: "",
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            spam = record.spam,
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun getContact(address: String, blockchainType: BlockchainType): Contact? {
        return contactsRepository.getContactsFiltered(blockchainType, addressQuery = address)
            .firstOrNull()
    }

    private fun mapped(address: String, blockchainType: BlockchainType): String {
        return getContact(address, blockchainType)?.name ?: evmLabelManager.mapped(address)
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
            subtitle = mapped(record.exchangeAddress, record.blockchainType),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            spam = record.spam,
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
            subtitle = mapped(record.exchangeAddress, record.blockchainType),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            spam = record.spam,
            icon = icon ?: doubleValueIconType(record.valueOut, record.valueIn)
        )
    }

    private fun createViewItemFromTronTransactionRecord(
        uid: String,
        timestamp: Long,
        contract: Contract?,
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = contract?.label ?: Translator.getString(R.string.Transactions_Unknown),
            subtitle = Translator.getString(R.string.Transactions_Unknown_Description),
            primaryValue = null,
            secondaryValue = null,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: TransactionViewItem.Icon.Platform(BlockchainType.Tron)
        )
    }

    private fun createViewItemFromEvmTransactionRecord(
        uid: String,
        timestamp: Long,
        blockchainType: BlockchainType,
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Unknown),
            subtitle = Translator.getString(R.string.Transactions_Unknown_Description),
            primaryValue = null,
            secondaryValue = null,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: TransactionViewItem.Icon.Platform(blockchainType)
        )
    }

    private fun createViewItemFromEvmOutgoingTransactionRecord(
        uid: String,
        value: TransactionValue,
        to: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        sentToSelf: Boolean,
        currencyValue: CurrencyValue?,
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val primaryValue = if (sentToSelf) {
            ColoredValue(getCoinString(value, true), ColorName.Leah)
        } else {
            getColoredValue(value, ColorName.Lucian)
        }

        val secondaryValue = singleValueSecondaryValue(value, currencyValue, nftMetadata)

        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Send),
            subtitle = Translator.getString(R.string.Transactions_To, mapped(to, blockchainType)),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(timestamp * 1000),
            sentToSelf = sentToSelf,
            spam = spam,
            icon = icon ?: singleValueIconType(value, nftMetadata)
        )
    }

    private fun getColoredValue(value: Any, color: ColorName): ColoredValue =
        when (value) {
            is TransactionValue -> ColoredValue(
                getCoinString(value),
                if (value.zeroValue) ColorName.Leah else color
            )

            is CurrencyValue -> ColoredValue(
                getCurrencyString(value),
                if (value.value.compareTo(BigDecimal.ZERO) == 0) ColorName.Grey else color
            )

            else -> ColoredValue(value.toString(), color)
        }

    private fun createViewItemFromEvmIncomingTransactionRecord(
        uid: String,
        value: TransactionValue,
        from: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValue = getColoredValue(value, ColorName.Remus)
        val secondaryValue = currencyValue?.let {
            getColoredValue(it, ColorName.Grey)
        }

        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Receive),
            subtitle = Translator.getString(
                R.string.Transactions_From,
                mapped(from, blockchainType)
            ),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: singleValueIconType(value)
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
            spam = record.spam,
            icon = icon ?: TransactionViewItem.Icon.Platform(record.blockchainType)
        )
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
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {
        val (primaryValue: ColoredValue?, secondaryValue: ColoredValue?) = getValues(
            incomingValues,
            outgoingValues,
            currencyValue,
            nftMetadata
        )
        val title = method ?: Translator.getString(R.string.Transactions_ContractCall)

        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = title,
            subtitle = mapped(contractAddress, blockchainType),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: iconType(blockchainType, incomingValues, outgoingValues, nftMetadata)
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
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): TransactionViewItem {

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
            val addresses = incomingEvents.mapNotNull { it.address }.toSet().toList()

            subTitle = if (addresses.size == 1) {
                Translator.getString(
                    R.string.Transactions_From, mapped(addresses.first(), blockchainType)
                )
            } else {
                Translator.getString(R.string.Transactions_Multiple)
            }
        } else {
            title = Translator.getString(R.string.Transactions_ExternalContractCall)
            subTitle = "---"
        }

        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = title,
            subtitle = subTitle,
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: iconType(blockchainType, incomingValues, outgoingValues, nftMetadata)
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
                mapped(it, record.blockchainType)
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
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            doubleSpend = record.conflictingHash != null,
            locked = locked,
            spam = record.spam,
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
                mapped(it, record.blockchainType)
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
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            sentToSelf = false,
            doubleSpend = record.conflictingHash != null,
            locked = locked,
            spam = record.spam,
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
            subtitle = Translator.getString(
                R.string.Transactions_To,
                mapped(record.to, record.blockchainType)
            ),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            sentToSelf = record.sentToSelf,
            spam = record.spam,
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun tryConvertToChangeNowViewItemSwap(
        transactionItem: TransactionItem,
        token: Token?,
        isIncoming: Boolean
    ) = if (token == null) {
        null
    } else if (isIncoming) {
        changeNowTransactionsStorage.getByTokenOut(
            token = token,
            timestamp = transactionItem.record.timestamp * 1000
        )
    } else {
        changeNowTransactionsStorage.getByTokenIn(
            token = token,
            timestamp = transactionItem.record.timestamp * 1000
        )
    }?.let {
        createViewItemFromChangeNowRecord(
            transaction = it,
            transactionItem = transactionItem,
            direct = !isIncoming
        )
    }

    private fun createViewItemFromChangeNowRecord(
        transaction: ChangeNowTransaction,
        transactionItem: TransactionItem,
        direct: Boolean
    ): TransactionViewItem {
        val iconIn = getIconForToken(
            coinUid = transaction.coinUidIn,
            blockchainType = transaction.blockchainTypeIn
        )
        val iconOut = getIconForToken(
            coinUid = transaction.coinUidOut,
            blockchainType = transaction.blockchainTypeOut
        )

        val transactionIcon = TransactionViewItem.Icon.Double(
            back = if (direct) iconIn else iconOut,
            front = if (direct) iconOut else iconIn
        )

        val valueInFormatted = getFormattedAmount(
            coinUid = transaction.coinUidIn,
            blockchainType = transaction.blockchainTypeIn,
            amount = transaction.amountIn,
            negative = true
        )

        val valueOutFormatted = getFormattedAmount(
            coinUid = transaction.coinUidOut,
            blockchainType = transaction.blockchainTypeOut,
            amount = transaction.amountOut,
            negative = false
        )

        val primaryValue = if (direct) {
            ColoredValue(valueInFormatted, ColorName.Lucian)
        } else {
            ColoredValue(valueOutFormatted, ColorName.Lucian)
        }
        val secondaryValue = if (direct) {
            ColoredValue(valueOutFormatted, ColorName.Remus)
        } else {
            ColoredValue(valueInFormatted, ColorName.Remus)
        }

        return TransactionViewItem(
            uid = transactionItem.record.uid,
            progress = 0f,
            title = Translator.getString(R.string.Transactions_Swap),
            subtitle = "ChangeNow",
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(transactionItem.record.timestamp * 1000),
            spam = false,
            icon = transactionIcon,
            transactionStatusUrl = ChangeNowHelper.CHANGE_NOW_URL to ChangeNowHelper.getViewTransactionUrl(transaction.transactionId)
        )
    }

    private fun getFormattedAmount(
        coinUid: String,
        blockchainType: String,
        amount: BigDecimal,
        negative: Boolean
    ): String {
        val sign = if (negative) "-" else "+"
        val coinCode =
            walletUseCase.getWallet(coinUid, blockchainType)?.coin?.code ?: coinUid.uppercase()

        val numberFormatted = numberFormatter.formatCoinShort(
            amount,
            coinCode,
            8
        )
        return "$sign $numberFormatted"
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
                mapped(record.from, record.blockchainType)
            ),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(record.timestamp * 1000),
            spam = record.spam,
            icon = icon ?: singleValueIconType(record.value)
        )
    }

    private fun createViewItemFromApproveTransactionRecord(
        uid: String,
        value: TransactionValue,
        spender: String,
        blockchainType: BlockchainType,
        timestamp: Long,
        currencyValue: CurrencyValue?,
        progress: Float?,
        spam: Boolean,
        icon: TransactionViewItem.Icon?
    ): TransactionViewItem {
        val primaryValueText: String
        val secondaryValueText: String?

        if (value.isMaxValue) {
            primaryValueText = "∞"
            secondaryValueText = if (value.coinCode.isEmpty()) "" else Translator.getString(
                R.string.Transaction_Unlimited_CoinCode,
                value.coinCode
            )
        } else {
            primaryValueText = getCoinString(value, hideSign = true)
            secondaryValueText = currencyValue?.let { getCurrencyString(it) }
        }

        val primaryValue = ColoredValue(primaryValueText, ColorName.Leah)
        val secondaryValue = secondaryValueText?.let { ColoredValue(it, ColorName.Grey) }

        return TransactionViewItem(
            uid = uid,
            progress = progress,
            title = Translator.getString(R.string.Transactions_Approve),
            subtitle = mapped(spender, blockchainType),
            primaryValue = primaryValue,
            secondaryValue = secondaryValue,
            showAmount = showAmount,
            date = Date(timestamp * 1000),
            spam = spam,
            icon = icon ?: singleValueIconType(value)
        )
    }

    private fun singleValueSecondaryValue(
        value: TransactionValue,
        currencyValue: CurrencyValue?,
        nftMetadata: Map<NftUid, NftAssetBriefMetadata>
    ): ColoredValue? =
        when (value) {
            is TransactionValue.NftValue -> {
                val text = nftMetadata[value.nftUid]?.name
                    ?: value.tokenName?.let { "$it #${value.nftUid.tokenId}" }
                    ?: "#${value.nftUid.tokenId}"
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
                secondaryValue =
                    singleValueSecondaryValue(transactionValue, currencyValue, nftMetadata)
            }

            // outgoing
            (incomingValues.isEmpty() && outgoingValues.size == 1) -> {
                val transactionValue = outgoingValues.first()
                primaryValue = getColoredValue(transactionValue, ColorName.Lucian)
                secondaryValue =
                    singleValueSecondaryValue(transactionValue, currencyValue, nftMetadata)
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
                primaryValue = getColoredValue(
                    outgoingValues.map { it.coinCode }.toSet().toList()
                        .joinToString(", "),
                    ColorName.Lucian
                )
                secondaryValue = getColoredValue(
                    Translator.getString(R.string.Transactions_Multiple),
                    ColorName.Grey
                )
            }

            // incoming multiple
            (incomingValues.isNotEmpty() && outgoingValues.isEmpty()) -> {
                primaryValue = getColoredValue(
                    incomingValues.map { it.coinCode }.toSet().toList()
                        .joinToString(", "),
                    ColorName.Remus
                )
                secondaryValue = getColoredValue(
                    Translator.getString(R.string.Transactions_Multiple),
                    ColorName.Grey
                )
            }

            else -> {
                primaryValue = if (incomingValues.size == 1) {
                    getColoredValue(incomingValues.first(), ColorName.Remus)
                } else {
                    getColoredValue(
                        incomingValues.joinToString(", ") { it.coinCode },
                        ColorName.Remus
                    )
                }
                secondaryValue = if (outgoingValues.size == 1) {
                    getColoredValue(outgoingValues.first(), ColorName.Remus)
                } else {
                    getColoredValue(
                        outgoingValues.map { it.coinCode }.toSet().toList().joinToString(", "),
                        ColorName.Lucian
                    )
                }
            }
        }
        return Pair(primaryValue, secondaryValue)
    }

    private fun getCurrencyString(currencyValue: CurrencyValue): String {
        return App.numberFormatter.formatFiatShort(
            currencyValue.value.abs(),
            currencyValue.currency.symbol,
            2
        )
    }

    private fun getCoinString(
        transactionValue: TransactionValue,
        hideSign: Boolean = false
    ): String {
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