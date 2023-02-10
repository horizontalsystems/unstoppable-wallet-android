package cash.p.terminal.core.adapters

import android.content.Context
import cash.p.terminal.core.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.core.BalanceData
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.entities.Wallet
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.Flowable
import io.reactivex.Single
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
) : BaseEvmAdapter(evmKitWrapper, wallet.decimal, coinManager) {

    private val transactionConverter = EvmTransactionConverter(coinManager, evmKitWrapper, wallet.transactionSource, baseToken, evmLabelManager)

    private val contractAddress: Address = Address(contractAddress)
    val eip20Kit: Erc20Kit = Erc20Kit.getInstance(context, this.evmKit, this.contractAddress)

    val pendingTransactions: List<TransactionRecord>
        get() = eip20Kit.getPendingTransactions().map { transactionConverter.transactionRecord(it) }

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

    override fun getTransactionData(amount: BigInteger, address: Address): TransactionData {
        return eip20Kit.buildTransferTransactionData(address, amount)
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
