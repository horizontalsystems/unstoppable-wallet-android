package io.horizontalsystems.walletkit.core.adapters

import android.content.Context
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IMoneroAccountsAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.core.ITransactionsAdapter
import io.horizontalsystems.walletkit.core.MoneroAccountInfo
import io.horizontalsystems.walletkit.core.MoneroUnspentOutput
import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.walletkit.core.managers.RestoreSettings
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.monerokit.Balance
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.Seed
import io.horizontalsystems.monerokit.SyncState
import io.horizontalsystems.monerokit.data.MoneroAccount
import io.horizontalsystems.monerokit.data.Subaddress
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MoneroAdapter(
    private val kit: MoneroKit,
    private val transactionsProvider: MoneroTransactionsProvider,
    private val transactionsAdapter: MoneroTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter, IMoneroAccountsAdapter,
    ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var balance = Balance(0, 0)

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = balance.toBalanceData()

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress(activeAccount)

    private val _activeAccountFlow = MutableStateFlow(0)
    override val activeAccountFlow = _activeAccountFlow.asStateFlow()

    override var activeAccount: Int
        get() = _activeAccountFlow.value
        set(value) {
            _activeAccountFlow.value = value
            // poke balance collectors so screens re-read account-scoped data
            balanceUpdatedSubject.onNext(Unit)
        }

    override val accountsFlow: StateFlow<List<MoneroAccountInfo>> = kit.accountsFlow
        .map { accounts -> accounts.map { it.toAccountInfo() } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, kit.accountsFlow.value.map { it.toAccountInfo() })

    override val activeAccountBalanceData: BalanceData
        get() {
            val account = kit.accountsFlow.value.firstOrNull { it.index == activeAccount }
                ?: return BalanceData(BigDecimal.ZERO)
            return account.balance.toBalanceData()
        }

    override fun createAccount(label: String?) {
        kit.createAccount(label)
    }

    override fun renameAccount(accountIndex: Int, label: String) {
        kit.setAccountLabel(accountIndex, label)
    }

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        kit.balanceFlow.collectWith(coroutineScope) {
            balance = it

            balanceUpdatedSubject.onNext(Unit)
        }

        kit.syncStateFlow.collectWith(coroutineScope) {
            balanceState = it.toAdapterState()

            balanceStateUpdatedSubject.onNext(Unit)
        }

        kit.allTransactionsFlow.collectWith(coroutineScope, transactionsProvider::onTransactions)

        kit.start()

        coroutineScope.launch {
            backgroundManager.stateFlow.collect {
                if (it == BackgroundManagerState.EnterBackground) {
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
        if (kit.syncStateFlow.value is SyncState.NotSynced) {
            kit.stop()
            kit.start()
        }
    }

    override val debugInfo: String
        get() = ""

    // JNI: refreshes the coins list under the wallet2 mutex, which the background
    // refresh can hold for seconds - never call on the main thread
    override suspend fun getUnspentOutputs(): List<MoneroUnspentOutput> = withContext(Dispatchers.IO) {
        val timestamps = kit.allTransactionsFlow.value.associate { it.hash to it.timestamp }
        kit.getUnspentOutputs()
            .filter { it.unlocked && !it.frozen }
            .map { output ->
                MoneroUnspentOutput(
                    keyImage = output.keyImage,
                    amount = output.amount.scaledDown(DECIMALS),
                    txHash = output.txHash,
                    address = kit.getSubaddress(0, output.subaddressIndex)?.address ?: "",
                    timestamp = timestamps[output.txHash]
                )
            }
    }

    override suspend fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        selectedOutputs: List<String>?
    ): String {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        return kit.send(amountInPiconero, address, memo, selectedOutputs, activeAccount)
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        val amountInPiconero = amount.movePointRight(DECIMALS).toLong()
        return kit.estimateFee(amountInPiconero, address, memo, activeAccount).scaledDown(DECIMALS)
    }

    fun getSubaddresses(): List<Subaddress> {
        return kit.getSubaddresses(activeAccount)
    }

    val statusInfo: Map<String, Any>
        get() = kit.statusInfo()

    companion object {
        const val DECIMALS = 12

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: MoneroNode
        ): MoneroAdapter {
            val birthdayHeightStr: String?
            val seed: Seed
            when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> {
                    birthdayHeightStr = restoreSettings.birthdayHeight?.toString()
                    seed = Seed.Bip39(accountType.words, accountType.passphrase)
                }

                is AccountType.MoneroWatchAccount -> {
                    birthdayHeightStr = accountType.restoreHeight.toString()
                    seed = Seed.WatchOnly(accountType.address, accountType.privateViewKey)
                }

                else -> throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")
            }

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> {
                    birthdayHeightStr ?: LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }

                AccountOrigin.Restored -> {
                    birthdayHeightStr ?: "0"
                }
            }

            val kit = MoneroKit.getInstance(
                context,
                seed,
                birthdayHeightOrDate,
                wallet.account.id,
                node.serialized,
                node.trusted
            )

            val transactionsProvider = MoneroTransactionsProvider()
            val transactionsAdapter = MoneroTransactionsAdapter(kit, transactionsProvider, wallet)

            return MoneroAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter,
                App.backgroundManager
            )
        }

        fun clear(walletId: String) {
            MoneroKit.deleteWallet(App.instance, walletId)
        }
    }
}

fun Long.scaledDown(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> {
        if (error is MoneroKit.SyncError.NotStarted) {
            AdapterState.Connecting
        } else {
            AdapterState.NotSynced(error)
        }
    }
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Connecting -> AdapterState.Connecting
    is SyncState.Syncing -> AdapterState.Syncing(
        progress = progress?.let {
            (it * 100).roundToInt().coerceAtMost(100)
        },
        blocksRemained = remainingBlocks
    )
}

fun AccountType.toMoneroSeed() = when (this) {
    is AccountType.Mnemonic -> Seed.Bip39(words, passphrase)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Monero Seed")
}

fun Balance.toBalanceData(): BalanceData {
    val available = unlocked.scaledDown(MoneroAdapter.DECIMALS)
    val pending = (all - unlocked).coerceAtLeast(0).scaledDown(MoneroAdapter.DECIMALS)
    return BalanceData(available, pending = pending)
}

fun MoneroAccount.toAccountInfo() = MoneroAccountInfo(
    index = index,
    label = label,
    balance = balance.all.scaledDown(MoneroAdapter.DECIMALS),
    unlocked = balance.unlocked.scaledDown(MoneroAdapter.DECIMALS),
)