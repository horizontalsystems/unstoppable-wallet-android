package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendOxyraAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class OxyraAdapter(
    private val kit: OxyraKit,
    private val transactionsProvider: OxyraTransactionsProvider,
    private val transactionsAdapter: OxyraTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendOxyraAdapter, ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var balance = OxyraBalance(0, 0)

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = balance.toBalanceData()

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        coroutineScope.launch {
            kit.balanceFlow.collect { newBalance ->
                balance = newBalance
                balanceUpdatedSubject.onNext(Unit)
            }
        }

        coroutineScope.launch {
            kit.syncStateFlow.collect { syncState ->
                balanceState = syncState.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }

        coroutineScope.launch {
            kit.allTransactionsFlow.collect { transactions ->
                transactionsProvider.onTransactions(transactions)
            }
        }

        coroutineScope.launch {
            kit.start()
        }

        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    kit.saveState()
                }
            }
        }
    }

    override fun stop() {
        kit.saveState()
        kit.stop()
        coroutineScope.cancel()
    }

    override fun refresh() {
        coroutineScope.launch {
            kit.start()
        }
    }

    override val debugInfo: String
        get() = ""

    override suspend fun send(amount: BigDecimal, address: String, memo: String?) {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        kit.send(amountInPiconero, address, memo)
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        return kit.estimateFee(amountInPiconero, address, memo).scaledDownOxyra(DECIMALS)
    }

    fun getSubaddresses(): List<OxyraSubaddress> {
        return kit.getSubaddresses()
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        const val DECIMALS = 12

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: OxyraNode
        ): OxyraAdapter {
            val birthdayHeightStr: String?
            val seed: OxyraSeed
            when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> {
                    birthdayHeightStr = restoreSettings.birthdayHeight?.toString()
                    seed = OxyraSeed.fromMnemonic(accountType.words.joinToString(" "))
                }

                is AccountType.OxyraWatchAccount -> {
                    birthdayHeightStr = accountType.restoreHeight.toString()
                    seed = OxyraSeed(
                        mnemonic = "",
                        privateSpendKey = "",
                        privateViewKey = accountType.privateViewKey,
                        publicSpendKey = "",
                        publicViewKey = ""
                    )
                }

                else -> throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")
            }

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> {
                    birthdayHeightStr ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                AccountOrigin.Restored -> {
                    birthdayHeightStr ?: "1"
                }
            }

            val kit = OxyraKit.getInstance(
                context,
                seed,
                birthdayHeightOrDate,
                wallet.account.id,
                node
            )

            val transactionsProvider = OxyraTransactionsProvider()
            val transactionsAdapter = OxyraTransactionsAdapter(kit, transactionsProvider, wallet)

            return OxyraAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter,
                App.backgroundManager
            )
        }

        fun clear(walletId: String) {
            OxyraKit.deleteWallet(App.instance, walletId)
        }
    }
}

// Data classes for Oxyra
data class OxyraBalance(val all: Long, val unlocked: Long)

data class OxyraSubaddress(
    val accountIndex: Int,
    val addressIndex: Int,
    val address: String,
    val label: String?
)

data class OxyraNode(
    val serialized: String,
    val trusted: Boolean
)

sealed class OxyraSyncState {
    object Synced : OxyraSyncState()
    object Connecting : OxyraSyncState()
    data class Syncing(val progress: Float?) : OxyraSyncState()
    data class NotSynced(val error: Throwable?) : OxyraSyncState()
}

// Extension functions
fun Long.scaledDownOxyra(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}

fun OxyraSyncState.toAdapterState(): AdapterState = when (this) {
    is OxyraSyncState.NotSynced -> AdapterState.NotSynced(error ?: Exception("Unknown error"))
    is OxyraSyncState.Synced -> AdapterState.Synced
    is OxyraSyncState.Connecting -> AdapterState.Syncing(connecting = true)
    is OxyraSyncState.Syncing -> AdapterState.Syncing(progress?.let {
        (it * 100).roundToInt().coerceAtMost(100)
    })
}

fun AccountType.toOxyraSeed(): OxyraSeed = when (this) {
    is AccountType.Mnemonic -> OxyraSeed.fromMnemonic(words.joinToString(" "))
    is AccountType.OxyraWatchAccount -> OxyraSeed(
        mnemonic = "",
        privateSpendKey = "",
        privateViewKey = privateViewKey,
        publicSpendKey = "",
        publicViewKey = ""
    )
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Oxyra Seed")
}

fun OxyraBalance.toBalanceData(): BalanceData {
    val available = unlocked.scaledDownOxyra(OxyraAdapter.DECIMALS)
    val pending = (all - unlocked).coerceAtLeast(0).scaledDownOxyra(OxyraAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}
