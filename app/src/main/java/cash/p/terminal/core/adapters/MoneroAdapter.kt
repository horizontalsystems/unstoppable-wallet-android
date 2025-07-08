package cash.p.terminal.core.adapters

import cash.p.terminal.core.ISendMoneroAdapter
import cash.p.terminal.core.managers.MoneroKitWrapper
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.wallet.entities.BalanceData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class MoneroAdapter(
    private val moneroKitWrapper: MoneroKitWrapper,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendMoneroAdapter {

    override val debugInfo: String
        get() = "Monero wallet: ${moneroKitWrapper.statusInfo()}"

    override val receiveAddress: String
        get() = moneroKitWrapper.getAddress()

    override val isMainNet: Boolean = true

    override val statusInfo: Map<String, Any>
        get() = moneroKitWrapper.statusInfo()

    // IAdapter

    override fun start() {
        // started via MoneroKitManager
    }

    override fun stop() {
        // stopped via MoneroKitManager
    }

    override suspend fun refresh() {
        // Refresh Monero wallet data
        moneroKitWrapper.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = moneroKitWrapper.syncState.value

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = moneroKitWrapper.syncState.map { }

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(moneroKitWrapper.getBalance(), decimal))

    override val balanceUpdatedFlow: Flow<Unit>
        get() = moneroKitWrapper.syncState.map { }

    override suspend fun send(
        amount: BigDecimal,
        address: String,
        memo: String?
    ) = moneroKitWrapper.send(amount, address, memo)

    override suspend fun estimateFee(
        amount: BigDecimal,
        address: String,
        memo: String?
    ): BigDecimal {
        return moneroKitWrapper.estimateFee(amount, address, memo).toBigDecimal()
            .movePointLeft(decimal)
    }

    companion object {
        const val decimal = 12 // Monero has 12 decimal places

        private fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
            return amount.movePointLeft(decimals).stripTrailingZeros()
        }

        fun balanceInBigDecimal(balance: Long?, decimal: Int): BigDecimal {
            balance?.toBigDecimal()?.let {
                return scaleDown(it, decimal)
            } ?: return BigDecimal.ZERO
        }
    }
}