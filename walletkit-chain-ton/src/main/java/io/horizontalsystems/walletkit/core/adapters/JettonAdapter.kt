package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.managers.TonKitWrapper
import io.horizontalsystems.walletkit.core.managers.toAdapterState
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.FriendlyAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class JettonAdapter(
    tonKitWrapper: TonKitWrapper,
    addressStr: String,
    wallet: Wallet,
) : BaseTonAdapter(tonKitWrapper, wallet.decimal) {

    private val address = Address.parse(addressStr)
    private var jettonBalance = tonKit.jettonBalanceMap[address]

    private val _balanceUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _balanceStateUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val balance: BigDecimal
        get() = jettonBalance?.balance?.toBigDecimal()?.movePointLeft(decimals)
            ?: BigDecimal.ZERO

    override var balanceState: AdapterState = AdapterState.Syncing()
    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = _balanceStateUpdatedFlow
    override val balanceData: BalanceData
        get() = BalanceData(balance)
    override val balanceUpdatedFlow: Flow<Unit>
        get() = _balanceUpdatedFlow

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun start() {
        coroutineScope.launch {
            tonKit.jettonBalanceMapFlow.collect { jettonBalanceMap ->
                jettonBalance = jettonBalanceMap[address]
                _balanceUpdatedFlow.tryEmit(Unit)
            }
        }
        coroutineScope.launch {
            tonKit.jettonSyncStateFlow.collect {
                balanceState = it.toAdapterState()
                _balanceStateUpdatedFlow.tryEmit(Unit)
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

        return estimateFee.toBigDecimal(9).stripTrailingZeros()
    }
}
