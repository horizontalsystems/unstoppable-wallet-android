package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendTonAdapter
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.core.managers.toAdapterState
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.FriendlyAddress
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.math.BigDecimal

class JettonAdapter(
    tonKitWrapper: TonKitWrapper,
    addressStr: String,
    wallet: Wallet,
) : BaseTonAdapter(tonKitWrapper, wallet.decimal), ISendTonAdapter {

    private val address = Address.parse(addressStr)
    private var jettonBalance = tonKit.jettonBalanceMap[address]

    private val balanceUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()
    private val balanceStateUpdatedSubject: PublishSubject<Unit> = PublishSubject.create()

    private val balance: BigDecimal
        get() = jettonBalance?.balance?.toBigDecimal()?.movePointLeft(decimals)
            ?: BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = balanceStateUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = balanceUpdatedSubject.toFlowable(BackpressureStrategy.BUFFER)

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun start() {
        coroutineScope.launch {
            tonKit.jettonBalanceMapFlow.collect { jettonBalanceMap ->
                jettonBalance = jettonBalanceMap[address]
                balanceUpdatedSubject.onNext(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.jettonSyncStateFlow.collect {
                balanceState = it.toAdapterState()
                balanceStateUpdatedSubject.onNext(Unit)
            }
        }
    }

    override fun stop() {
        coroutineScope.cancel()
    }

    override fun refresh() {
    }

    override val availableBalance: BigDecimal
        get() = balance

    override suspend fun send(amount: BigDecimal, address: FriendlyAddress, memo: String?) {
        tonKit.send(
            jettonBalance?.walletAddress!!,
            address,
            amount.movePointRight(decimals).toBigInteger(),
            memo
        )
    }

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: FriendlyAddress,
        memo: String?,
    ): BigDecimal {
        val estimateFee = tonKit.estimateFee(
            jettonBalance?.walletAddress!!,
            address,
            amount.movePointRight(decimals).toBigInteger(),
            memo
        )

        return estimateFee.toBigDecimal(decimals).stripTrailingZeros()
    }
}
