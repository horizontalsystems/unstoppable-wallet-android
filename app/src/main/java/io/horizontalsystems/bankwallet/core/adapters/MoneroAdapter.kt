package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import android.util.Log
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ISendMoneroAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager.MoneroNode
import io.horizontalsystems.bankwallet.core.managers.RestoreSettings
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.Seed
import io.horizontalsystems.monerokit.SyncState
import io.horizontalsystems.monerokit.data.Subaddress
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

class MoneroAdapter(
    private val kit: MoneroKit,
    private val transactionsProvider: MoneroTransactionsProvider,
    private val transactionsAdapter: MoneroTransactionsAdapter,
    private val backgroundManager: BackgroundManager,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter,
    ITransactionsAdapter by transactionsAdapter {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private var totalBalance = BigDecimal.ZERO

    override var balanceState: AdapterState = kit.syncStateFlow.value.toAdapterState()

    override val balanceData: BalanceData
        get() = BalanceData(totalBalance)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val receiveAddress: String
        get() = kit.receiveAddress

    override val isMainNet: Boolean
        get() = true

    override fun start() {
        kit.balanceFlow.collectWith(coroutineScope) {
            totalBalance = it.scaledDown(DECIMALS)

            balanceUpdatedSubject.onNext(Unit)
        }

        kit.syncStateFlow.collectWith(coroutineScope) {
            balanceState = it.toAdapterState()

            balanceStateUpdatedSubject.onNext(Unit)
        }

        kit.allTransactionsFlow.collectWith(coroutineScope, transactionsProvider::onTransactions)

        kit.start()

        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground) {
                    kit.saveState()
                }
            }
        }
    }

    override fun stop() {
        Log.e("AAA", "moneroAdapter: stop()")
        kit.stop()

        coroutineScope.cancel()
    }

    override fun refresh() {
        Log.e("AAA", "moneroAdapter: refresh()")
        kit.stop()
        kit.start()
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
        return kit.estimateFee(amountInPiconero, address, memo).scaledDown(DECIMALS)
    }

    fun getSubaddresses(): List<Subaddress> {
        return kit.getSubaddresses()
    }

    companion object {
        const val DECIMALS = 12

        fun create(
            context: Context,
            wallet: Wallet,
            restoreSettings: RestoreSettings,
            node: MoneroNode
        ): MoneroAdapter {
            val mnemonic = (wallet.account.type as? AccountType.Mnemonic)
                ?: throw IllegalStateException("Unsupported account type: ${wallet.account.type.javaClass.simpleName}")

            val birthdayHeightOrDate: String = when (wallet.account.origin) {
                AccountOrigin.Created -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                AccountOrigin.Restored -> (restoreSettings.birthdayHeight ?: 0).toString()
            }

            Log.e("eee", "birthdayHeightOrDate: $birthdayHeightOrDate, node: ${node.serialized}")

            val kit = MoneroKit.getInstance(
                context,
                Seed.Bip39(mnemonic.words, mnemonic.passphrase),
                birthdayHeightOrDate,
                wallet.account.id,
                node.serialized
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
    }
}

fun Long.scaledDown(decimals: Int): BigDecimal {
    return this.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
}

fun SyncState.toAdapterState(): AdapterState = when (this) {
    is SyncState.NotSynced -> AdapterState.NotSynced(error)
    is SyncState.Synced -> AdapterState.Synced
    is SyncState.Syncing -> AdapterState.Syncing(progress?.let {
        (it * 100).roundToInt().coerceAtMost(100)
    })
}

fun AccountType.toMoneroSeed() = when (this) {
    is AccountType.Mnemonic -> Seed.Bip39(words, passphrase)
    else -> throw IllegalArgumentException("Account type ${this.javaClass.simpleName} can not be converted to Monero Seed")
}