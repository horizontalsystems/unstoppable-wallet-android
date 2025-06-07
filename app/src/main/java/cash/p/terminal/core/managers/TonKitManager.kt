package cash.p.terminal.core.managers

import android.util.Log
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.Status
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.Value
import cash.p.terminal.modules.transactionInfo.TransactionViewItemFactoryHelper
import cash.p.terminal.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.TokenType
import cash.z.ecc.android.sdk.ext.fromHex
import com.tonapps.blockchain.ton.contract.WalletV4R2Contract
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.core.entities.CurrencyValue
import io.horizontalsystems.hdwalletkit.Curve
import io.horizontalsystems.hdwalletkit.HDWallet
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
import kotlinx.coroutines.runBlocking
import org.ton.api.pub.PublicKeyEd25519

class TonKitManager(
    private val backgroundManager: BackgroundManager,
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage
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
                is AccountType.TonAddress,
                is AccountType.HardwareCard,
                is AccountType.Mnemonic -> {
                    createKitInstance(getTonWallet(account), account)
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
        when (account.type) {
            is AccountType.HardwareCard,
            is AccountType.TonAddress,
            is AccountType.Mnemonic -> {
                createKitInstance(getTonWallet(account), account)
            }

            else -> throw UnsupportedAccountException()
        }

    private fun getTonWallet(account: Account): TonWallet {
        return when (val accountType = account.type) {
            is AccountType.TonAddress,
            is AccountType.Mnemonic -> {
                accountType.toTonWallet()
            }

            is AccountType.HardwareCard -> {
                runBlocking {
                    val hardwarePublicKey =
                        hardwarePublicKeyStorage.getKey(account.id, BlockchainType.Ton, TokenType.Native)
                    if (hardwarePublicKey == null || hardwarePublicKey.type != HardwarePublicKeyType.ADDRESS) {
                        throw UnsupportedException("Hardware card does not have a public key for TON")
                    }

                    val walletV4R2Contract =
                        WalletV4R2Contract(publicKey = PublicKeyEd25519(hardwarePublicKey.key.value.fromHex()))
                    TonWallet.WatchOnly(walletV4R2Contract.address.toString(userFriendly = true))
                }
            }

            else -> throw UnsupportedAccountException()
        }
    }

    private fun createKitInstance(
        tonWallet: TonWallet,
        account: Account,
    ): TonKitWrapper {
        val kit = TonKit.getInstance(tonWallet, Network.MainNet, App.instance, account.id)

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
        Log.d("TonKitManager", "start")
        tonKitWrapper?.tonKit?.start()
        job = scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    Log.d("TonKitManager", "EnterForeground")
                    tonKitWrapper?.tonKit?.let { kit ->
                        delay(1000)
                        kit.refresh()
                    }
                } else if (state == BackgroundManagerState.EnterBackground) {
                    Log.d("TonKitManager", "EnterBackground")
                    tonKitWrapper?.tonKit?.stop()
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
                            cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfo_Memo),
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
                            cash.p.terminal.strings.helpers.Translator.getString(R.string.TransactionInfo_Memo),
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
