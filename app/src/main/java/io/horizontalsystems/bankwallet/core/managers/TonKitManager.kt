package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.UnsupportedAccountException
import io.horizontalsystems.bankwallet.core.adapters.TonTransactionRecord
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Status
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoViewItem.Value
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.hdwalletkit.Curve
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.core.TonWallet
import io.horizontalsystems.tonkit.models.Jetton
import io.horizontalsystems.tonkit.models.Network
import io.horizontalsystems.tonkit.models.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TonKitManager(
    private val backgroundManager: BackgroundManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow

    var tonKitWrapper: TonKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.update { value != null }
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set

    val statusInfo: Map<String, Any>?
        get() = tonKitWrapper?.tonKit?.statusInfo()

    @Synchronized
    fun getTonKitWrapper(account: Account): TonKitWrapper {
        if (this.tonKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tonKitWrapper == null) {
            val accountType = account.type
            this.tonKitWrapper = when (accountType) {
                is AccountType.Mnemonic -> {
                    createKitInstance(accountType, account)
                }

                is AccountType.TonAddress -> {
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
        return this.tonKitWrapper!!
    }

    fun getNonActiveTonKitWrapper(account: Account) =
        when (val accountType = account.type) {
            is AccountType.Mnemonic -> {
                createKitInstance(accountType, account)
            }

            is AccountType.TonAddress -> {
                createKitInstance(accountType, account)
            }

            else -> throw UnsupportedAccountException()
        }

    private fun createKitInstance(
        accountType: AccountType.Mnemonic,
        account: Account,
    ): TonKitWrapper {
        val kit = TonKit.getInstance(accountType.toTonWallet(), Network.MainNet, App.instance, account.id)

        return TonKitWrapper(kit)
    }

    private fun createKitInstance(
        accountType: AccountType.TonAddress,
        account: Account,
    ): TonKitWrapper {
        val kit = TonKit.getInstance(accountType.toTonWallet(), Network.MainNet, App.instance, account.id)

        return TonKitWrapper(kit)
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
        tonKitWrapper?.tonKit?.stop()
        job?.cancel()
        tonKitWrapper = null
        currentAccount = null
    }

    private suspend fun start() {
        tonKitWrapper?.tonKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    tonKitWrapper?.tonKit?.let { kit ->
                        delay(1000)
                        kit.refresh()
                    }
                }
            }
        }
    }
}

object TonHelper {
    fun getViewItemsForAction(
        action: TonTransactionRecord.Action,
        rates: Map<String, CurrencyValue>,
        blockchainType: BlockchainType,
        hideAmount: Boolean
    ): List<TransactionInfoViewItem> {

        val itemsForAction = mutableListOf<TransactionInfoViewItem>()

        when (val actionType = action.type) {
            is TonTransactionRecord.Action.Type.Send -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = actionType.to,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        sentToSelf = actionType.sentToSelf,
                        blockchainType = blockchainType,
                    )
                )
                actionType.comment?.let {
                    itemsForAction.add(
                        Value(
                            Translator.getString(R.string.TransactionInfo_Memo),
                            it
                        )
                    )
                }
            }

            is TonTransactionRecord.Action.Type.Receive -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = actionType.value,
                        fromAddress = actionType.from,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
                actionType.comment?.let {
                    itemsForAction.add(
                        Value(
                            Translator.getString(R.string.TransactionInfo_Memo),
                            it
                        )
                    )
                }
            }

            is TonTransactionRecord.Action.Type.Burn -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = null,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TonTransactionRecord.Action.Type.Mint -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getReceiveSectionItems(
                        value = actionType.value,
                        fromAddress = TransactionViewItemFactoryHelper.zeroAddress,
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        blockchainType = blockchainType,
                    )
                )
            }

            is TonTransactionRecord.Action.Type.Swap -> {
                itemsForAction.addAll(
                    getSwapEventSectionItems(
                        valueIn = actionType.valueIn,
                        valueOut = actionType.valueOut,
                        rates = rates,
                        amount = null,
                        hideAmount = hideAmount,
                        hasRecipient = false
                    )
                )
            }

            is TonTransactionRecord.Action.Type.ContractDeploy -> {
//                        case let .contractDeploy(interfaces):
//                            viewItems = [
//                                .actionTitle(iconName: nil, iconDimmed: false, title: "transactions.contract_deploy".localized, subTitle: interfaces.joined(separator: ", ")),
//                            ]
            }

            is TonTransactionRecord.Action.Type.ContractCall -> {
//                        case let .contractCall(address, value, operation):
//                            viewItems = [
//                                .actionTitle(iconName: record.source.blockchainType.iconPlain32, iconDimmed: false, title: "transactions.contract_call".localized, subTitle: operation),
//                                .to(value: address, valueTitle: nil, contactAddress: nil)
//                            ]
//
//                            viewItems.append(contentsOf: sendSection(source: record.source, transactionValue: value, to: nil, rates: item.rates, balanceHidden: balanceHidden))
            }

            is TonTransactionRecord.Action.Type.Unsupported -> {
                itemsForAction.add(Value("Action", actionType.type))
            }
        }

        if (action.status == TransactionStatus.Failed) {
            itemsForAction.add(Status(action.status))
        }

        return itemsForAction


    }
}

class TonKitWrapper(val tonKit: TonKit)

fun TonKit.statusInfo() = buildMap {
    put("Sync State", syncStateFlow.value.toAdapterState())
    put("Event Sync State", eventSyncStateFlow.value.toAdapterState())
    put("Jetton Sync State", jettonSyncStateFlow.value.toAdapterState())
}

val Jetton.tokenType
    get() = TokenType.Jetton(address.toUserFriendly(true))

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> AdapterState.NotSynced(error)
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Syncing -> AdapterState.Syncing()
}

fun AccountType.toTonWalletFullAccess(): TonWallet.FullAccess {
    val toTonWallet = toTonWallet()

    return toTonWallet as? TonWallet.FullAccess ?: throw IllegalArgumentException("Watch Only")
}

fun AccountType.toTonWallet() = when (this) {
    is AccountType.Mnemonic -> {
        val hdWallet = HDWallet(seed, 607, HDWallet.Purpose.BIP44, Curve.Ed25519)
        val privateKey = hdWallet.privateKey(0)
        var privateKeyBytes = privateKey.privKeyBytes
        if (privateKeyBytes.size > 32) {
            privateKeyBytes = privateKeyBytes.copyOfRange(1, privateKeyBytes.size)
        }
        TonWallet.Seed(privateKeyBytes)

    }
    is AccountType.TonAddress -> {
        TonWallet.WatchOnly(address)
    }
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to TonKit.WalletType")
}
