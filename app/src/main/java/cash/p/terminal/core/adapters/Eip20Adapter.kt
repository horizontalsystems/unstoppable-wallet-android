package cash.p.terminal.core.adapters

import android.content.Context
import cash.p.terminal.core.App
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.StackingManager
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.math.BigInteger

internal class Eip20Adapter(
    context: Context,
    evmTransactionRepository: EvmTransactionRepository,
    contractAddress: String,
    baseToken: Token,
    coinManager: ICoinManager,
    private val wallet: Wallet,
    evmLabelManager: EvmLabelManager,
    private val stackingManager: StackingManager
) : BaseEvmAdapter(evmTransactionRepository, wallet.decimal, coinManager) {

    private val transactionConverter = EvmTransactionConverter(
        coinManager = coinManager,
        evmTransactionRepository = evmTransactionRepository,
        source = wallet.transactionSource,
        spamManager = App.spamManager,
        baseToken = baseToken,
        evmLabelManager = evmLabelManager
    )

    private val contractAddress: Address = Address(contractAddress)
    private val eip20Kit: Erc20Kit = evmTransactionRepository.buildErc20Kit(context, this.contractAddress)

    val pendingTransactions: List<TransactionRecord>
        get() = eip20Kit.getPendingTransactions().map { transactionConverter.transactionRecord(it) }

    // IAdapter

    override fun start() {
        stackingManager.loadInvestmentData(wallet, receiveAddress)
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override suspend fun refresh() {
        stackingManager.loadInvestmentData(wallet, receiveAddress, true)
        eip20Kit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(eip20Kit.syncState)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = merge(eip20Kit.syncStateFlowable.asFlow(), stackingManager.unpaidFlow.filterNotNull())
            .map { }

    override val balanceData: BalanceData
        get() = BalanceData(
                available = balanceInBigDecimal(eip20Kit.balance, decimal),
                stackingUnpaid = stackingManager.unpaidFlow.value ?: BigDecimal.ZERO
            )

   override val balanceUpdatedFlow: Flow<Unit>
        get() = merge(eip20Kit.balanceFlowable.asFlow(), stackingManager.unpaidFlow.filterNotNull())
            .map { }

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

    fun allowance(
        spenderAddress: Address,
        defaultBlockParameter: DefaultBlockParameter
    ): Single<BigDecimal> {
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
            val networkTypes = listOf(
                Chain.Ethereum,
                Chain.BinanceSmartChain,
                Chain.Polygon,
                Chain.Avalanche,
                Chain.Optimism,
                Chain.ArbitrumOne,
                Chain.Gnosis,
            )

            networkTypes.forEach {
                Erc20Kit.clear(App.instance, it, walletId)
            }
        }
    }

}
