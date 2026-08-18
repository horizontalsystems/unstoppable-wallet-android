package io.horizontalsystems.walletkit.core.adapters

import android.content.Context
import io.horizontalsystems.walletkit.core.IEip20ApproveAdapter
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.EvmTransactionData
import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.managers.EvmKitWrapper
import io.horizontalsystems.walletkit.core.managers.EvmLabelManager
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.BigInteger

class Eip20Adapter(
    context: Context,
    evmKitWrapper: EvmKitWrapper,
    contractAddress: String,
    baseToken: Token,
    coinManager: ICoinManager,
    wallet: Wallet,
    evmLabelManager: EvmLabelManager
) : BaseEvmAdapter(evmKitWrapper, wallet.decimal, coinManager), IEip20ApproveAdapter {

    override fun buildApproveTransactionData(spender: String, amount: java.math.BigDecimal): EvmTransactionData =
        buildApproveTransactionData(Address(spender), amount).toCarrier()

    override fun buildApproveUnlimitedTransactionData(spender: String): EvmTransactionData =
        buildApproveUnlimitedTransactionData(Address(spender)).toCarrier()

    override fun buildRevokeTransactionData(spender: String): EvmTransactionData =
        buildRevokeTransactionData(Address(spender)).toCarrier()


    private val transactionConverter = EvmTransactionConverter(coinManager, evmKitWrapper, wallet.transactionSource, baseToken, evmLabelManager)

    private val contractAddress: Address = Address(contractAddress)
    val eip20Kit: Erc20Kit = Erc20Kit.getInstance(context, this.evmKit, this.contractAddress)

    val pendingTransactions: List<TransactionRecord>
        get() = eip20Kit.getPendingTransactions().map { runBlocking { transactionConverter.transactionRecord(it) } }

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        eip20Kit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(eip20Kit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = eip20Kit.syncStateFlowable.map { }

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(eip20Kit.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = eip20Kit.balanceFlowable.map { Unit }

    // ISendEthereumAdapter

    override fun getTransactionData(amount: BigDecimal, address: Address): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return eip20Kit.buildTransferTransactionData(address, amountBigInt)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing()
    }

    fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter): Single<BigDecimal> {
        return eip20Kit.getAllowanceAsync(spenderAddress, defaultBlockParameter)
                .map {
                    scaleDown(it.toBigDecimal())
                }
    }

    fun buildRevokeTransactionData(spenderAddress: Address): TransactionData {
        return eip20Kit.buildApproveTransactionData(spenderAddress, BigInteger.ZERO)
    }

    fun buildApproveTransactionData(spenderAddress: Address, amount: BigDecimal): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return eip20Kit.buildApproveTransactionData(spenderAddress, amountBigInt)
    }

    fun buildApproveUnlimitedTransactionData(spenderAddress: Address): TransactionData {
        val max = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE)
        return eip20Kit.buildApproveTransactionData(spenderAddress, max)
    }

    companion object {
        fun clear(walletId: String) {
            // Same canonical list as EvmAdapter.clear — see the note there.
            EvmBlockchainManager.blockchainTypes.forEach { blockchainType ->
                Erc20Kit.clear(App.instance, EvmKitManagerRegistry.getChain(blockchainType), walletId)
            }
        }
    }

}

private fun TransactionData.toCarrier() = EvmTransactionData(
    to = to.eip55,
    value = value,
    input = input,
)
