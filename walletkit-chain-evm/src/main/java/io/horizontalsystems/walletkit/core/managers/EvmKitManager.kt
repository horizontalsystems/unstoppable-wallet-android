package io.horizontalsystems.walletkit.core.managers

import android.util.Log
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.UnsupportedAccountException
import io.horizontalsystems.walletkit.entities.Account
import io.horizontalsystems.walletkit.entities.AccountType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.merkleiokit.MerkleTransactionAdapter
import io.horizontalsystems.nftkit.core.NftKit
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.TokenFactory.UnsupportedChainError
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URI

class EvmKitManager(
    val chain: Chain,
    private val backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    init {
        coroutineScope.launch {
            syncSourceManager.syncSourceFlow.collect { blockchain ->
                handleUpdateNetwork(blockchain)
            }
        }
    }

    // Runs off the sync-source subscription while getEvmKitWrapper()/unlink() may be inside the
    // monitor. Without it, the wrapper can be nulled between that getter's null check and its !!,
    // and the blockchainType read below is a check-then-act on the same field. Callers already
    // holding the monitor re-enter it — Zano's manager has had this since it was written.
    @Synchronized
    private fun handleUpdateNetwork(blockchainType: BlockchainType) {
        if (blockchainType != evmKitWrapper?.blockchainType) return

        stopEvmKit()

        _evmKitUpdatedFlow.tryEmit(Unit)
    }

    private val _kitStartedFlow = MutableStateFlow(false)
    val kitStartedFlow: StateFlow<Boolean> = _kitStartedFlow.asStateFlow()

    var evmKitWrapper: EvmKitWrapper? = null
        private set(value) {
            field = value

            _kitStartedFlow.value = value != null
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val _evmKitUpdatedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val evmKitUpdatedFlow: SharedFlow<Unit> = _evmKitUpdatedFlow.asSharedFlow()

    val statusInfo: Map<String, Any>?
        get() = evmKitWrapper?.evmKit?.statusInfo()

    @Synchronized
    fun getEvmKitWrapper(account: Account, blockchainType: BlockchainType): EvmKitWrapper {
        if (evmKitWrapper != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKitWrapper == null) {
            val accountType = account.type
            evmKitWrapper = createKitInstance(accountType, account, blockchainType)
            useCount = 0
            currentAccount = account
            subscribeToEvents()
        }

        useCount++
        return this.evmKitWrapper!!
    }

    private fun createKitInstance(
        accountType: AccountType,
        account: Account,
        blockchainType: BlockchainType
    ): EvmKitWrapper {
        val syncSource = syncSourceManager.getSyncSource(blockchainType)

        val address: Address
        var signer: Signer? = null

        when (accountType) {
            is AccountType.Mnemonic -> {
                val seed: ByteArray = accountType.seed
                address = Signer.address(seed, chain)
                signer = Signer.getInstance(seed, chain)
            }

            is AccountType.EvmPrivateKey -> {
                address = Signer.address(accountType.key)
                signer = Signer.getInstance(accountType.key, chain)
            }

            is AccountType.EvmAddress -> {
                address = Address(accountType.address)
            }

            is AccountType.Passkey -> {
                // Smart-account: the address comes from the app's AA layer via the
                // registered resolver; the kit runs in watch mode (signing goes
                // through the ERC-4337 pipeline, not the kit signer).
                address = SmartAccountAddressResolver.provider?.evmAddress(account, blockchainType)
                    ?: throw UnsupportedAccountException()
            }

            else -> throw UnsupportedAccountException()
        }

        val evmKit = EthereumKit.getInstance(
            App.instance,
            address,
            chain,
            syncSource.rpcSource,
            evmTransactionSource(blockchainType, App.appConfigProvider),
            account.id
        )

        // A registered config provider may own the kit's transaction pipeline
        // (syncers + decorators) for this account; defaults only otherwise.
        val configured = EvmKitConfigResolver.provider?.configure(evmKit, account, blockchainType) == true
        if (!configured) {
            Erc20Kit.addTransactionSyncer(evmKit)
            Erc20Kit.addDecorators(evmKit)

            UniswapKit.addDecorators(evmKit)
            try {
                UniswapV3Kit.addDecorators(evmKit)
            } catch (e: UnsupportedChainError.NoWethAddress) {
                //do nothing
            }
            OneInchKit.addDecorators(evmKit)
        }

        val nftKit: NftKit? = null
//        var nftKit: NftKit? = null
//        val supportedNftTypes = blockchainType.supportedNftTypes
//        if (supportedNftTypes.isNotEmpty()) {
//            val nftKitInstance = NftKit.getInstance(App.instance, evmKit)
//            supportedNftTypes.forEach {
//                when (it) {
//                    NftType.Eip721 -> {
//                        nftKitInstance.addEip721TransactionSyncer()
//                        nftKitInstance.addEip721Decorators()
//                    }
//                    NftType.Eip1155 -> {
//                        nftKitInstance.addEip1155TransactionSyncer()
//                        nftKitInstance.addEip1155Decorators()
//                    }
//                }
//            }
//            nftKit = nftKitInstance
//        }

        val merkleTransactionAdapter = MerkleTransactionAdapter.getInstance(
            merkleIoPubKey = "pk_mbs_5f012edb2cf20a96b49429a3ed285a45",
            address = address,
            chain = chain,
            context = App.instance,
            walletId = account.id,
            transactionManager = evmKit.transactionManager,
            sourceTag = "unstoppable-wallet-android"
        )

        merkleTransactionAdapter?.registerInKit(evmKit)

        evmKit.start()

        return EvmKitWrapper(evmKit, nftKit, blockchainType, signer, merkleTransactionAdapter)
    }

    @Synchronized
    fun unlink(account: Account) {
        if (account == currentAccount) {
            useCount -= 1

            if (useCount < 1) {
                Log.d("AAA", "stopEvmKit()")
                stopEvmKit()
            }
        }
    }

    private fun subscribeToEvents() {
        job = coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        evmKitWrapper?.evmKit?.let { kit ->
                            kit.onEnterForeground()
                            delay(1000)
                            kit.refresh()
                        }
                    }

                    BackgroundManagerState.EnterBackground -> {
                        evmKitWrapper?.evmKit?.onEnterBackground()
                    }
                }
            }
        }
    }

    @Synchronized
    private fun stopEvmKit() {
        job?.cancel()
        evmKitWrapper?.evmKit?.stop()
        evmKitWrapper = null
        currentAccount = null
    }

    data class SignedTx(val hex: String, val txHash: String)
}

val RpcSource.uris: List<URI>
    get() = when (this) {
        is RpcSource.WebSocket -> listOf(uri)
        is RpcSource.Http -> uris
    }

class EvmKitWrapper(
    val evmKit: EthereumKit,
    val nftKit: NftKit?,
    val blockchainType: BlockchainType,
    val signer: Signer?,
    val merkleTransactionAdapter: MerkleTransactionAdapter?
) {

    suspend fun send(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
        mevProtectionEnabled: Boolean
    ): FullTransaction {
        if (signer == null) throw Exception()
        if (mevProtectionEnabled && merkleTransactionAdapter == null) throw Exception()

        val rawTransaction = evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
        val signature = signer.signature(rawTransaction)

        return if (mevProtectionEnabled && merkleTransactionAdapter != null) {
            merkleTransactionAdapter.send(rawTransaction, signature)
        } else {
            evmKit.send(rawTransaction, signature)
        }
    }

    suspend fun sign(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
    ): EvmKitManager.SignedTx {
        if (signer == null) throw IllegalStateException("Signer not available")
        val rawTransaction = evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
        val signature = signer.signature(rawTransaction)
        val encoded = TransactionBuilder.encode(rawTransaction, signature, evmKit.chain.id)
        return EvmKitManager.SignedTx(hex = encoded.toHexString(), txHash = CryptoUtils.sha3(encoded).toHexString())
    }
}

/**
 * Kit-side registry for the per-chain EvmKitManager/EvmAccountManager pairs; the kit-free
 * EvmBlockchainManager stays in core while this moves with the EVM chain module.
 */
object EvmKitManagerRegistry {
    private val evmKitManagersMap = mutableMapOf<BlockchainType, Pair<EvmKitManager, EvmAccountManager>>()

    private val evmAccountManagerFactory by lazy {
        io.horizontalsystems.walletkit.core.factories.EvmAccountManagerFactory(
            App.accountManager,
            App.walletManager,
            App.marketKit,
            App.tokenAutoEnableManager,
        )
    }

    fun getChain(blockchainType: BlockchainType): Chain = when (blockchainType) {
        BlockchainType.Ethereum -> Chain.Ethereum
        BlockchainType.BinanceSmartChain -> Chain.BinanceSmartChain
        BlockchainType.Polygon -> Chain.Polygon
        BlockchainType.Avalanche -> Chain.Avalanche
        BlockchainType.Optimism -> Chain.Optimism
        BlockchainType.Base -> Chain.Base
        BlockchainType.ZkSync -> Chain.ZkSync
        BlockchainType.RobinhoodChain -> Chain.RobinhoodChain
        BlockchainType.ArbitrumOne -> Chain.ArbitrumOne
        BlockchainType.Gnosis -> Chain.Gnosis
        BlockchainType.Fantom -> Chain.Fantom
        else -> throw IllegalArgumentException("Unsupported blockchain type ${'$'}blockchainType")
    }

    @Synchronized
    private fun managers(blockchainType: BlockchainType): Pair<EvmKitManager, EvmAccountManager> =
        evmKitManagersMap.getOrPut(blockchainType) {
            val evmKitManager = EvmKitManager(getChain(blockchainType), App.backgroundManager, App.evmSyncSourceManager)
            val evmAccountManager = evmAccountManagerFactory.evmAccountManager(blockchainType, evmKitManager)
            Pair(evmKitManager, evmAccountManager)
        }

    fun getEvmKitManager(blockchainType: BlockchainType): EvmKitManager = managers(blockchainType).first

    fun getEvmAccountManager(blockchainType: BlockchainType): EvmAccountManager = managers(blockchainType).second
}
