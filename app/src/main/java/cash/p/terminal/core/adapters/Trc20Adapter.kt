package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.INativeBalanceProvider
import cash.p.terminal.core.ISendTronAdapter
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.TronKitWrapper
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.tronkit.TronKit.SyncState
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.TriggerSmartContract
import io.horizontalsystems.tronkit.transaction.Fee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.math.BigInteger

class Trc20Adapter(
    tronKitWrapper: TronKitWrapper,
    contractAddress: String,
    wallet: Wallet,
    baseToken: Token,
) : BaseTronAdapter(tronKitWrapper, wallet.decimal), ISendTronAdapter, INativeBalanceProvider {

    private val contractAddress: Address = Address.fromBase58(contractAddress)

    private val coinManager: ICoinManager by inject(ICoinManager::class.java)
    private val evmLabelManager: EvmLabelManager by inject(EvmLabelManager::class.java)
    private val transactionConverter = TronTransactionConverter(
        coinManager,
        tronKitWrapper,
        wallet.transactionSource,
        baseToken,
        evmLabelManager
    )

    // IAdapter

    override fun start() {
        // started via TronKitManager
    }

    override fun stop() {
        // stopped via TronKitManager
    }

    override suspend fun refresh() {
        // refreshed via TronKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(tronKit.syncState)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = tronKit.syncStateFlow.map { }

    override val balanceData: BalanceData
        get() = BalanceData(
            balanceInBigDecimal(
                tronKit.getTrc20Balance(contractAddress.base58),
                decimal
            )
        )

    override val balanceUpdatedFlow: Flow<Unit>
        get() = tronKit.getTrc20BalanceFlow(contractAddress.base58).map { }

    // ISendTronAdapter

    override val trxBalanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.trxBalance, TronAdapter.decimal))

    // INativeBalanceProvider

    override val nativeBalanceData: BalanceData
        get() = trxBalanceData

    override val nativeBalanceUpdatedFlow: Flow<Unit>
        get() = tronKit.trxBalanceFlow.map { }

    override suspend fun estimateFee(amount: BigDecimal, to: Address): List<Fee> =
        withContext(Dispatchers.IO) {
            val amountBigInt = amount.movePointRight(decimal).toBigInteger()
            val contract =
                tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amountBigInt)
            tronKit.estimateFee(contract)
        }

    override suspend fun send(amount: BigDecimal, to: Address, feeLimit: Long?): String {
        if (signer == null) throw Exception()
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        val contract = tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amountBigInt)

        return tronKit.send(contract, signer, feeLimit)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing()
    }

    suspend fun allowance(spenderAddress: String): BigDecimal {
        val tronAddress = Address.fromBase58(spenderAddress)

        return balanceInBigDecimal(tronKit.getTrc20Allowance(contractAddress, tronAddress), decimal)
    }

    fun approveTrc20TriggerSmartContract(
        spenderAddress: String,
        requiredAllowance: BigDecimal
    ): TriggerSmartContract {
        val tronAddress = Address.fromBase58(spenderAddress)
        val amountBigInt = requiredAllowance.movePointRight(decimal).toBigInteger()

        return tronKit.approveTrc20TriggerSmartContract(contractAddress, tronAddress, amountBigInt)
    }

    fun approveTrc20TriggerSmartContractUnlim(spenderAddress: String): TriggerSmartContract {
        val tronAddress = Address.fromBase58(spenderAddress)
        val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)

        return tronKit.approveTrc20TriggerSmartContract(contractAddress, tronAddress, max)
    }

    suspend fun getPendingTransactions(): List<TronTransactionRecord> {
        return tronKit.getPendingTransactions(listOf(listOf(contractAddress.base58))).map {
            transactionConverter.transactionRecord(it)
        }
    }

}
