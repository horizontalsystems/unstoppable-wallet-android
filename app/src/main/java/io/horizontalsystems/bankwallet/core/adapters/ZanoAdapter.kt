package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendMoneroAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.ZanoKitManager
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.zanokit.BalanceInfo
import io.horizontalsystems.zanokit.SendAmount
import io.horizontalsystems.zanokit.SyncState
import io.horizontalsystems.zanokit.ZanoKit
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ZanoAdapter(
    private val kit: ZanoKit,
    private val assetId: String,
    private val transactionsProvider: ZanoTransactionsProvider,
    private val transactionsAdapter: ZanoTransactionsAdapter,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter, ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = (kit.balance(assetId) ?: BalanceInfo(assetId, 0, 0, 0, 0)).toBalanceData()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        coroutineScope.launch {
            kit.syncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            kit.balancesFlow.collect {
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            kit.transactionsFlow.collect { txs ->
                transactionsProvider.onTransactions(txs)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
        kit.refresh()
    }

    override val debugInfo: String
        get() = ""

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        val atomicAmount = amount.movePointRight(DECIMALS).toLong()
        kit.send(
            toAddress = address,
            assetId = assetId,
            amount = SendAmount.Value(atomicAmount),
            memo = memo,
        )
    }

    override suspend fun estimateFee(amount: BigDecimal, address: String, memo: String?): BigDecimal =
        kit.estimateFee().scaledDown(DECIMALS)

    companion object {
        const val DECIMALS = 12

        fun create(wallet: Wallet, zanoKitManager: ZanoKitManager, restoreSettings: RestoreSettings): ZanoAdapter {
            val account = wallet.account
            account.type as? AccountType.Mnemonic
                ?: throw IllegalStateException("Unsupported account type")

            val assetId = when (val type = wallet.token.type) {
                is TokenType.Native -> ZanoKit.ZANO_ASSET_ID
                is TokenType.ZanoAsset -> type.reference
                else -> throw IllegalStateException("Unsupported token type: $type")
            }

            val creationTimestamp = when (account.origin) {
                AccountOrigin.Created -> {
                    val blockHeight = restoreSettings.birthdayHeight ?: ZanoKit.restoreHeightForDate(java.util.Date())
                    ZanoKit.dateForRestoreHeight(blockHeight).time / 1000
                }
                AccountOrigin.Restored -> {
                    val blockHeight = restoreSettings.birthdayHeight ?: 0L
                    if (blockHeight > 0) ZanoKit.dateForRestoreHeight(blockHeight).time / 1000 else 0L
                }
            }

            val wrapper = zanoKitManager.getZanoKitWrapper(account, creationTimestamp)
            val kit = wrapper.kit
            val provider = ZanoTransactionsProvider(assetId)
            val txAdapter = ZanoTransactionsAdapter(kit, provider, wallet)

            return ZanoAdapter(kit, assetId, provider, txAdapter)
        }
    }
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Syncing -> AdapterState.Syncing(
        progress = progress.coerceIn(0, 100),
        blocksRemained = remainingBlocks
    )
    is SyncState.Connecting -> AdapterState.Connecting
    is SyncState.NotSynced.NotStarted,
    is SyncState.NotSynced.NoNetwork -> AdapterState.Connecting
    is SyncState.NotSynced.StartError -> AdapterState.NotSynced(Exception(message))
    is SyncState.NotSynced.StatusError -> AdapterState.NotSynced(Exception(message))
}

fun BalanceInfo.toBalanceData(): BalanceData {
    val available = unlocked.scaledDown(ZanoAdapter.DECIMALS)
    val pending = (awaitingIn + awaitingOut).coerceAtLeast(0).scaledDown(ZanoAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}
