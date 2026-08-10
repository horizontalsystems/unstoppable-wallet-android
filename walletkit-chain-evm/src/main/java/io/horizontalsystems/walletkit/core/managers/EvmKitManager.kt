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
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
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
            syncSourceManager.syncSourceObservable.asFlow().collect { blockchain ->
                handleUpdateNetwork(blockchain)
            }
        }
    }

    private fun handleUpdateNetwork(blockchainType: BlockchainType) {
        if (blockchainType != evmKitWrapper?.blockchainType) return

        stopEvmKit()

        evmKitUpdatedSubject.onNext(Unit)
    }

    private val kitStartedSubject = BehaviorSubject.createDefault(false)
    val kitStartedObservable: Observable<Boolean> = kitStartedSubject

    var evmKitWrapper: EvmKitWrapper? = null
        private set(value) {
            field = value

            kitStartedSubject.onNext(value != null)
        }

    private var useCount = 0
    var currentAccount: Account? = null
        private set
    private val evmKitUpdatedSubject = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedSubject

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

    fun sendSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
        mevProtectionEnabled: Boolean
    ): Single<FullTransaction> {
        if (signer == null) return Single.error(Exception())
        if (mevProtectionEnabled && merkleTransactionAdapter == null) return Single.error(Exception())

        return evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
            .flatMap { rawTransaction ->
                val signature = signer.signature(rawTransaction)

                if (mevProtectionEnabled && merkleTransactionAdapter != null) {
                    merkleTransactionAdapter.send(rawTransaction, signature)
                } else {
                    evmKit.send(rawTransaction, signature)
                }
            }
    }

    fun signSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
    ): Single<EvmKitManager.SignedTx> {
        if (signer == null) return Single.error(IllegalStateException("Signer not available"))
        return evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce)
            .map { rawTransaction ->
                val signature = signer.signature(rawTransaction)
                val encoded = TransactionBuilder.encode(rawTransaction, signature, evmKit.chain.id)
                EvmKitManager.SignedTx(hex = encoded.toHexString(), txHash = CryptoUtils.sha3(encoded).toHexString())
            }
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
