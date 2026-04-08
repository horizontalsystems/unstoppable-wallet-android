package cash.p.terminal.core.managers

import android.util.Log
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.UnsupportedException
import cash.p.terminal.core.toFixedSize
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.Status
import cash.p.terminal.modules.transactionInfo.TransactionInfoViewItem.Value
import cash.p.terminal.modules.transactionInfo.TransactionViewItemFactoryHelper
import cash.p.terminal.modules.transactionInfo.TransactionViewItemFactoryHelper.getSwapEventSectionItems
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.tangem.signer.HardwareWalletTonSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.TokenType
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
import io.horizontalsystems.tronkit.hexStringToByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString
import org.ton.kotlin.crypto.PublicKeyEd25519

class TonKitManager(
    private val backgroundManager: BackgroundManager,
    private val hardwarePublicKeyStorage: HardwarePublicKeyStorage,
    private val backgroundKeepAliveManager: BackgroundKeepAliveManager
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
    fun getTonKitWrapper(
        account: Account,
        blockchainType: BlockchainType?,
    ): TonKitWrapper {
        if (this.tonKitWrapper != null && currentAccount != account) {
            stop()
        }

        if (this.tonKitWrapper == null) {
            this.tonKitWrapper =
                createKitInstance(
                    account.toTonWallet(
                        hardwarePublicKeyStorage,
                        blockchainType,
                    ), account
                )
            scope.launch {
                start()
            }
            useCount = 0
            currentAccount = account
        }

        useCount++
        return this.tonKitWrapper!!
    }

    fun getTonWallet(
        account: Account,
        blockchainType: BlockchainType?,
    ) = account.toTonWallet(
        hardwarePublicKeyStorage,
        blockchainType,
    )

    fun getNonActiveTonKitWrapper(
        account: Account,
        blockchainType: BlockchainType?,
    ) = createKitInstance(
        getTonWallet(
            account,
            blockchainType,
        ), account
    )

    private fun createKitInstance(
        tonWallet: TonWallet,
        account: Account,
    ): TonKitWrapper {
        return TonKitWrapper(
            TonKit.getInstance(
                tonWallet,
                Network.MainNet,
                App.instance,
                account.id
            ),
            tonWallet
        )
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
                    if (!backgroundKeepAliveManager.isKeepAlive(BlockchainType.Ton)) {
                        Log.d("TonKitManager", "EnterBackground")
                        tonKitWrapper?.tonKit?.stop()
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
        hideAmount: Boolean,
        showCopyWarning: Boolean = false,
    ): List<TransactionInfoViewItem> {

        val itemsForAction = mutableListOf<TransactionInfoViewItem>()

        when (val actionType = action.type) {
            is TonTransactionRecord.Action.Type.Send -> {
                itemsForAction.addAll(
                    TransactionViewItemFactoryHelper.getSendSectionItems(
                        value = actionType.value,
                        toAddress = listOf(actionType.to),
                        coinPrice = rates[actionType.value.coinUid],
                        hideAmount = hideAmount,
                        sentToSelf = actionType.sentToSelf,
                        blockchainType = blockchainType,
                        showCopyWarning = showCopyWarning,
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
                        showCopyWarning = showCopyWarning,
                        value = actionType.value,
                        fromAddress = actionType.from,
                        toAddress = actionType.to?.let(::listOf),
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
                        toAddress = actionType.to?.let(::listOf),
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

class TonKitWrapper(val tonKit: TonKit, val tonWallet: TonWallet)

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

fun Account.toTonWalletFullAccess(
    hardwarePublicKeyStorage: HardwarePublicKeyStorage,
    blockchainType: BlockchainType?,
): TonWallet.FullAccess {
    val toTonWallet = toTonWallet(hardwarePublicKeyStorage, blockchainType)

    return toTonWallet as? TonWallet.FullAccess ?: throw IllegalArgumentException("Watch Only")
}

fun Account.toTonWallet(
    hardwarePublicKeyStorage: HardwarePublicKeyStorage,
    blockchainType: BlockchainType?,
) = when (this.type) {
    is AccountType.Mnemonic -> {
        val hdWallet = HDWallet(
            (this.type as AccountType.Mnemonic).seed,
            607,
            HDWallet.Purpose.BIP44,
            Curve.Ed25519
        )
        val privateKey = hdWallet.privateKey(0)
        TonWallet.Seed(privateKey.privKeyBytes.toFixedSize(32))
    }

    is AccountType.TonAddress -> {
        TonWallet.WatchOnly((this.type as AccountType.TonAddress).address)
    }

    is AccountType.HardwareCard -> {
        runBlocking {
            val resolvedBlockchainType = blockchainType
                ?: throw IllegalArgumentException("Blockchain type is null")
            // All TON tokens share the same key/address (derivation path m/44'/607'/0').
            // Try Native first, then fall back to any available key for this blockchain
            // (covers case where only Jetton tokens were added without native TON).
            val hardwarePublicKey = hardwarePublicKeyStorage.getKey(
                accountId = id,
                blockchainType = resolvedBlockchainType,
                tokenType = TokenType.Native
            ) ?: hardwarePublicKeyStorage.getAllPublicKeys(id).firstOrNull {
                it.blockchainType == resolvedBlockchainType.uid
            }
            if (hardwarePublicKey == null || hardwarePublicKey.type != HardwarePublicKeyType.ADDRESS) {
                throw UnsupportedException("Hardware public key not found for TON (accountId=$id, blockchainType=$resolvedBlockchainType)")
            }
            TonWallet.FullAccess(
                PublicKeyEd25519(ByteString(hardwarePublicKey.key.value.hexStringToByteArray())),
                HardwareWalletTonSigner(hardwarePublicKey)
            )
        }
    }

    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to TonKit.WalletType")
}
