package cash.p.terminal.data.repository

import android.content.Context
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.HistoricalSyncState
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.LegacyGasPriceProvider
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService.Companion.BLOCKS_COUNT
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService.Companion.LAST_N_RECOMMENDED_BASE_FEES
import cash.p.terminal.modules.evmfee.eip1559.Eip1559GasPriceService.Companion.REWARD_PERCENTILE
import io.horizontalsystems.ethereumkit.core.eip1559.Eip1559GasPriceProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

internal class EvmTransactionRepository(
    private val evmBlockchainManager: EvmBlockchainManager
) {
    private var evmKitWrapper: EvmKitWrapper? = null
    private var _blockchainType: BlockchainType? = null

    private val evmKit: EthereumKit
        get() = requireNotNull(evmKitWrapper).evmKit

    val lastBlockHeight: Long?
        get() = evmKit.lastBlockHeight

    val lastBlockHeightFlowable: Flowable<Long>
        get() = evmKit.lastBlockHeightFlowable

    val transactionsSyncState: SyncState
        get() = evmKit.transactionsSyncState

    val transactionsSyncStateFlowable: Flowable<SyncState>
        get() = evmKit.transactionsSyncStateFlowable

    val receiveAddress: Address
        get() = evmKit.receiveAddress

    val chain: Chain
        get() = evmKit.chain

    val syncState: SyncState
        get() = evmKit.syncState

    val syncStateFlowable: Flowable<SyncState>
        get() = evmKit.syncStateFlowable

    val historicalSyncState: StateFlow<HistoricalSyncState>
        get() = evmKit.historicalSyncState

    val forwardSyncState: StateFlow<EthereumKit.ForwardSyncState>
        get() = evmKit.forwardSyncState

    val accountState: AccountState?
        get() = evmKit.accountState

    val accountStateFlowable: Flowable<AccountState>
        get() = evmKit.accountStateFlowable

    val transactionSyncSourceStorage
        get() = evmKit.transactionSyncSourceStorage

    fun debugInfo(): String = evmKit.debugInfo()

    fun statusInfo(): Map<String, Any> = evmKit.statusInfo()

    fun setup(
        account: Account,
        blockchainType: BlockchainType
    ): EvmKitWrapper {
        _blockchainType = blockchainType
        return evmBlockchainManager.getEvmKitManager(blockchainType)
            .getEvmKitWrapper(account, blockchainType)
            .also { evmKitWrapper = it }
    }

    fun buildErc20Kit(
        context: Context,
        contractAddress: Address
    ): Erc20Kit = Erc20Kit.getInstance(context, evmKit, contractAddress)

    fun getBlockchainType(): BlockchainType = requireNotNull(_blockchainType)

    fun getTagTokenContractAddresses(): List<String> = evmKit.getTagTokenContractAddresses()

    suspend fun getFullTransactionsAsync(
        tags: List<List<String>>,
        fromHash: ByteArray? = null,
        limit: Int? = null
    ): List<FullTransaction> = evmKit.getFullTransactionsAsync(tags, fromHash, limit).await()

    fun getFullTransactionsFlowable(tags: List<List<String>>): Flow<List<FullTransaction>> =
        evmKit.getFullTransactionsFlowable(tags).asFlow()

    /**
     * Estimates the fee for a native token transfer.
     * Returns the fee in wei as BigInteger.
     */
    suspend fun estimateNativeTransferFee(): BigInteger {
        val gasLimit = BigInteger.valueOf(evmKit.defaultGasLimit)
        val gasPrice = if (chain.isEIP1559Supported) {
            getEip1559GasPrice()
        } else {
            getLegacyGasPrice()
        }
        return gasLimit * gasPrice
    }

    private suspend fun getEip1559GasPrice(): BigInteger {
        val provider = Eip1559GasPriceProvider(evmKit)
        val feeHistory = provider.feeHistorySingle(BLOCKS_COUNT, DefaultBlockParameter.Latest, REWARD_PERCENTILE).await()

        val baseFee = feeHistory.baseFeePerGas.takeLast(LAST_N_RECOMMENDED_BASE_FEES).maxOrNull() ?: 0L
        var priorityFeeSum = 0L
        var priorityFeeCount = 0
        feeHistory.reward.forEach { rewards ->
            rewards.firstOrNull()?.let {
                priorityFeeSum += it
                priorityFeeCount++
            }
        }
        val priorityFee = if (priorityFeeCount > 0) priorityFeeSum / priorityFeeCount else 0L

        return BigInteger.valueOf(baseFee + priorityFee)
    }

    private suspend fun getLegacyGasPrice(): BigInteger {
        val provider = LegacyGasPriceProvider(evmKit)
        val gasPrice = provider.gasPriceSingle().await()
        return BigInteger.valueOf(gasPrice)
    }
}
