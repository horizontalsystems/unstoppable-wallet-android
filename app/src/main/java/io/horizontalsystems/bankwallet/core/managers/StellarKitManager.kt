package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.Network
import io.horizontalsystems.stellarkit.StellarKit
import io.horizontalsystems.stellarkit.StellarWallet
import io.horizontalsystems.stellarkit.SyncState
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StellarKitManager(
    private val backgroundManager: BackgroundManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var stellarKitWrapper: StellarKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = stellarKitWrapper?.stellarKit?.statusInfo()

    @Synchronized
    fun getStellarKitWrapper(account: Account): StellarKitWrapper {
        if (this.stellarKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.stellarKitWrapper == null) {
            val accountType = account.type
            this.stellarKitWrapper = when (accountType) {
                is AccountType.Mnemonic,
                is AccountType.StellarAddress -> {
                    createKitInstance(accountType, account)
                }

                else -> throw UnsupportedAccountException()
            }
            scope.launch {
                start()
            }
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.stellarKitWrapper!!
    }

    private fun createKitInstance(accountType: AccountType, account: Account): StellarKitWrapper {
        val kit = StellarKit.getInstance(accountType.toStellarWallet(), Network.MainNet, App.instance, account.id)

        return StellarKitWrapper(kit)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                stop()
            }
        }
    }

    private fun stop() {
        stellarKitWrapper?.stellarKit?.stop()
        job?.cancel()
        stellarKitWrapper = null
        currentAccount = null
    }

    private suspend fun start() {
        stellarKitWrapper?.stellarKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    stellarKitWrapper?.stellarKit?.let { kit ->
                        delay(1000)
                        kit.refresh()
                    }
                }
            }
        }
    }
}

//object TonHelper {
//    fun getViewItemsForAction(
//        action: TonTransactionRecord.Action,
//        rates: Map<String, CurrencyValue>,
//        blockchainType: BlockchainType,
//        hideAmount: Boolean,
//        showHistoricalRate: Boolean
//    ): List<TransactionInfoViewItem> {
//
//        val itemsForAction = mutableListOf<TransactionInfoViewItem>()
//
//        when (val actionType = action.type) {
//            is TonTransactionRecord.Action.Type.Send -> {
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getSendSectionItems(
//                        value = actionType.value,
//                        toAddress = actionType.to,
//                        coinPrice = rates[actionType.value.coinUid],
//                        hideAmount = hideAmount,
//                        sentToSelf = actionType.sentToSelf,
//                        blockchainType = blockchainType,
//                        showHistoricalRate = showHistoricalRate
//                    )
//                )
//                actionType.comment?.let {
//                    itemsForAction.add(
//                        Value(
//                            Translator.getString(R.string.TransactionInfo_Memo),
//                            it
//                        )
//                    )
//                }
//            }
//
//            is TonTransactionRecord.Action.Type.Receive -> {
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
//                        value = actionType.value,
//                        fromAddress = actionType.from,
//                        coinPrice = rates[actionType.value.coinUid],
//                        hideAmount = hideAmount,
//                        blockchainType = blockchainType,
//                        showHistoricalRate = showHistoricalRate
//                    )
//                )
//                actionType.comment?.let {
//                    itemsForAction.add(
//                        Value(
//                            Translator.getString(R.string.TransactionInfo_Memo),
//                            it
//                        )
//                    )
//                }
//            }
//
//            is TonTransactionRecord.Action.Type.Burn -> {
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getSendSectionItems(
//                        value = actionType.value,
//                        toAddress = null,
//                        coinPrice = rates[actionType.value.coinUid],
//                        hideAmount = hideAmount,
//                        blockchainType = blockchainType,
//                    )
//                )
//            }
//
//            is TonTransactionRecord.Action.Type.Mint -> {
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
//                        value = actionType.value,
//                        fromAddress = TransactionViewItemFactoryHelper.zeroAddress,
//                        coinPrice = rates[actionType.value.coinUid],
//                        hideAmount = hideAmount,
//                        blockchainType = blockchainType,
//                    )
//                )
//            }
//
//            is TonTransactionRecord.Action.Type.Swap -> {
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getSwapEventSectionItems(
//                        valueIn = actionType.valueIn,
//                        valueOut = actionType.valueOut,
//                        rates = rates,
//                        amount = null,
//                        hideAmount = hideAmount,
//                        hasRecipient = false
//                    )
//                )
//            }
//
//            is TonTransactionRecord.Action.Type.ContractDeploy -> {
//                itemsForAction.add(
//                    TransactionInfoViewItem.Transaction(
//                        leftValue = Translator.getString(R.string.Transactions_ContractDeploy),
//                        rightValue = actionType.interfaces.joinToString(),
//                        icon = null,
//                    )
//                )
//
//            }
//
//            is TonTransactionRecord.Action.Type.ContractCall -> {
//                itemsForAction.add(
//                    TransactionInfoViewItem.Transaction(
//                        leftValue = Translator.getString(R.string.Transactions_ContractCall),
//                        rightValue = actionType.operation,
//                        icon = TransactionViewItem.Icon.Platform(blockchainType).iconRes,
//                    )
//                )
//
//                itemsForAction.add(
//                    TransactionInfoViewItem.Address(
//                        Translator.getString(R.string.TransactionInfo_To),
//                        actionType.address,
//                        false,
//                        blockchainType,
//                        StatSection.AddressTo
//                    )
//                )
//
//                itemsForAction.addAll(
//                    TransactionViewItemFactoryHelper.getSendSectionItems(
//                        value = actionType.value,
//                        toAddress = null,
//                        coinPrice = rates[actionType.value.coinUid],
//                        hideAmount = hideAmount,
//                        blockchainType = blockchainType,
//                    )
//                )
//            }
//
//            is TonTransactionRecord.Action.Type.Unsupported -> {
//                itemsForAction.add(Value("Action", actionType.type))
//            }
//        }
//
//        if (action.status == TransactionStatus.Failed) {
//            itemsForAction.add(Status(action.status))
//        }
//
//        return itemsForAction
//
//
//    }
//}

class StellarKitWrapper(val stellarKit: StellarKit)

fun StellarKit.statusInfo(): Map<String, Any> = TODO()
//    buildMap {
//    put("Sync State", syncStateFlow.value.toAdapterState())
//    put("Event Sync State", eventSyncStateFlow.value.toAdapterState())
//    put("Jetton Sync State", jettonSyncStateFlow.value.toAdapterState())
//}

val StellarAsset.Asset.tokenType
    get() = TokenType.Asset(code, issuer)

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> AdapterState.NotSynced(error)
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Syncing -> AdapterState.Syncing()
}

//fun AccountType.toTonWalletFullAccess(): TonWallet.FullAccess {
//    val toTonWallet = toTonWallet()
//
//    return toTonWallet as? TonWallet.FullAccess ?: throw IllegalArgumentException("Watch Only")
//}

fun AccountType.toStellarWallet() = when (this) {
    is AccountType.Mnemonic -> StellarWallet.Seed(seed)
    is AccountType.StellarAddress -> StellarWallet.WatchOnly(address)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to StellarWallet.Wallet")
}
