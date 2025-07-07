package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.tronkit.TronKit.SyncState
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.TriggerSmartContract
import io.horizontalsystems.tronkit.transaction.Fee
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.BigInteger

class Trc20Adapter(
    tronKitWrapper: TronKitWrapper,
    contractAddress: String,
    wallet: Wallet,
    coinManager: ICoinManager,
    baseToken: Token,
    evmLabelManager: EvmLabelManager
) : BaseTronAdapter(tronKitWrapper, wallet.decimal) {

    private val contractAddress: Address = Address.fromBase58(contractAddress)
    private val transactionConverter = TronTransactionConverter(coinManager, tronKitWrapper, wallet.transactionSource, baseToken, evmLabelManager)

    // IAdapter

    override fun start() {
        // started via TronKitManager
    }

    override fun stop() {
        // stopped via TronKitManager
    }

    override fun refresh() {
        // refreshed via TronKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(tronKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = tronKit.syncStateFlow.map { }.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.getTrc20Balance(contractAddress.base58), decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = tronKit.getTrc20BalanceFlow(contractAddress.base58).map { }.asFlowable()

    // ISendTronAdapter

    override val trxBalanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.trxBalance, TronAdapter.decimal))

    override suspend fun estimateFee(amount: BigDecimal, to: Address): List<Fee> = withContext(Dispatchers.IO) {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        val contract = tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amountBigInt)
        tronKit.estimateFee(contract)
    }

    override suspend fun send(amount: BigDecimal, to: Address, feeLimit: Long?) {
        if (signer == null) throw Exception()
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        val contract = tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amountBigInt)

        tronKit.send(contract, signer, feeLimit)
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

    fun approveTrc20TriggerSmartContract(spenderAddress: String, requiredAllowance: BigDecimal): TriggerSmartContract {
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
