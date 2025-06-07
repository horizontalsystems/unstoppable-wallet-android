package cash.p.terminal.data.repository

import android.content.Context
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmKitWrapper
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.SecretString
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await

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

    val accountState: AccountState?
        get() = evmKit.accountState

    val accountStateFlowable: Flowable<AccountState>
        get() = evmKit.accountStateFlowable

    fun debugInfo(): String = evmKit.debugInfo()

    fun statusInfo(): Map<String, Any> = evmKit.statusInfo()

    fun setup(
        account: Account,
        blockchainType: BlockchainType
    ) {
        _blockchainType = blockchainType
        evmKitWrapper = evmBlockchainManager.getEvmKitManager(blockchainType)
            .getEvmKitWrapper(account, blockchainType)
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
}