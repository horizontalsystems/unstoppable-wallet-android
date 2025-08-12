package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import android.util.Log
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IBalanceAdapter
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.monerokit.MoneroKit
import io.horizontalsystems.monerokit.SyncState
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import java.math.BigDecimal
import kotlin.math.roundToInt

class MoneroAdapter(
    private val kit: MoneroKit,
    private val transactionsProvider: MoneroTransactionsProvider,
    private val transactionsAdapter: MoneroTransactionsAdapter,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ITransactionsAdapter by transactionsAdapter {

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
            Log.e("eee", "syncstate: $it")

            balanceState = it.toAdapterState()

            balanceStateUpdatedSubject.onNext(Unit)
        }

        kit.allTransactionsFlow.collectWith(coroutineScope, transactionsProvider::onTransactions)

        kit.start()
    }

    override fun stop() {
        Log.e("AAA", "moneroAdapter: stop()")
        kit.stop()

        coroutineScope.cancel()
    }

    override fun refresh() {
        Log.e("AAA", "moneroAdapter: refresh()")
//        TODO("not implemented")
    }

    override val debugInfo: String
        get() = TODO("Not yet implemented")


    companion object {
        const val DECIMALS = 12

        fun create(context: Context, wallet: Wallet): MoneroAdapter {
            val words = when (val accountType = wallet.account.type) {
                is AccountType.Mnemonic -> accountType.words
                else -> throw IllegalStateException("Unsupported account type: ${accountType.javaClass.simpleName}")
            }

            val kit = MoneroKit.getInstance(
                context,
                words,
                "",
                wallet.account.id
            )

            val transactionsProvider = MoneroTransactionsProvider()
            val transactionsAdapter = MoneroTransactionsAdapter(kit, transactionsProvider, wallet)

            return MoneroAdapter(
                kit,
                transactionsProvider,
                transactionsAdapter
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
    is SyncState.Syncing -> AdapterState.Syncing(progress?.let { (it * 100).roundToInt().coerceAtMost(100) })
}