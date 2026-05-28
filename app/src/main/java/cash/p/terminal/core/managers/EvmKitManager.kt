package cash.p.terminal.core.managers

import android.os.Handler
import android.os.Looper
import cash.p.terminal.core.App
import cash.p.terminal.core.onPollingStarted
import cash.p.terminal.core.onPollingStopped
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.tangem.common.CustomXPubKeyAddressParser
import cash.p.terminal.tangem.domain.model.AddressBytesWithPublicKey
import cash.p.terminal.tangem.signer.HardwareWalletEvmSigner
import cash.p.terminal.trezor.domain.TrezorDeepLinkManager
import cash.p.terminal.trezor.signer.TrezorEvmSigner
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.entities.HardwarePublicKey
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.merkleiokit.MerkleTransactionAdapter
import io.horizontalsystems.nftkit.core.NftKit
import io.horizontalsystems.oneinchkit.OneInchKit
import io.horizontalsystems.uniswapkit.TokenFactory.UnsupportedChainError
import io.horizontalsystems.uniswapkit.UniswapKit
import io.horizontalsystems.uniswapkit.UniswapV3Kit
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import org.koin.java.KoinJavaComponent.inject
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class EvmKitManager(
    val chain: Chain,
    private val backgroundManager: BackgroundManager,
    private val syncSourceManager: EvmSyncSourceManager,
    private val backgroundKeepAliveManager: BackgroundKeepAliveManager
) {
    private val hardwarePublicKeyStorage: IHardwarePublicKeyStorage
            by inject(IHardwarePublicKeyStorage::class.java)
    private val trezorDeepLinkManager: TrezorDeepLinkManager
            by inject(TrezorDeepLinkManager::class.java)

    private val lifecycleMutex = Mutex()
    private val pollingSessionCount = AtomicInteger(0)
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

    private var useCount = AtomicInteger(0)
    var currentAccount: Account? = null
        private set
    private val evmKitUpdatedSubject = PublishSubject.create<Unit>()

    val evmKitUpdatedObservable: Observable<Unit>
        get() = evmKitUpdatedSubject

    val statusInfo: Map<String, Any>?
        get() = evmKitWrapper?.evmKit?.statusInfo()

    suspend fun getEvmKitWrapper(
        account: Account,
        blockchainType: BlockchainType
    ): EvmKitWrapper = lifecycleMutex.withLock {
        if (evmKitWrapper != null && currentAccount != account) {
            stopEvmKit()
        }

        if (this.evmKitWrapper == null) {
            val accountType = account.type
            evmKitWrapper = createKitInstance(
                accountType = accountType,
                account = account,
                blockchainType = blockchainType
            )
            useCount.set(0)
            currentAccount = account
            subscribeToEvents()
        }
        useCount.incrementAndGet()
        requireNotNull(this.evmKitWrapper)
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

            is AccountType.HardwareCard -> {
                val (publicKey, addressWithPublicKey) = resolveHardwareAddress(account.id, blockchainType)
                address = Address(addressWithPublicKey.addressBytes)
                signer = HardwareWalletEvmSigner(
                    address = address,
                    publicKey = publicKey,
                    chain = chain,
                    expectedPublicKeyBytes = addressWithPublicKey.publicKey
                )
            }

            is AccountType.TrezorDevice -> {
                val (publicKey, addressWithPublicKey) = resolveHardwareAddress(account.id, blockchainType)
                address = Address(addressWithPublicKey.addressBytes)
                signer = TrezorEvmSigner(
                    address = address,
                    chain = chain,
                    derivationPath = publicKey.derivationPath,
                    deepLinkManager = trezorDeepLinkManager
                )
            }

            is AccountType.EvmAddress -> {
                address = Address(accountType.address)
            }

            else -> throw UnsupportedAccountException()
        }

        val evmKit = EthereumKit.getInstance(
            application = App.instance,
            address = address,
            chain = chain,
            rpcSource = syncSource.rpcSource,
            transactionSource = syncSource.transactionSource,
            walletId = account.id,
            scanHistoricalEip20 = account.origin == AccountOrigin.Restored
        )

        Erc20Kit.addTransactionSyncer(evmKit)
        Erc20Kit.addDecorators(evmKit)

        UniswapKit.addDecorators(evmKit)
        try {
            UniswapV3Kit.addDecorators(evmKit)
        } catch (e: UnsupportedChainError.NoWethAddress) {
            //do nothing
        }
        OneInchKit.addDecorators(evmKit)

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
            merkleIoPubKey = AppConfigProvider.merkleIoKey,
            address = address,
            chain = chain,
            context = App.instance,
            walletId = account.id,
            transactionManager = evmKit.transactionManager,
            sourceTag = "pcash-wallet-android",
            transactionSyncSourceStorage = evmKit.transactionSyncSourceStorage
        )
        merkleTransactionAdapter?.registerInKit(evmKit)

        evmKit.start()

        return EvmKitWrapper(
            evmKit = evmKit,
            nftKit = nftKit,
            blockchainType = blockchainType,
            signer = signer,
            merkleTransactionAdapter = merkleTransactionAdapter
        )
    }

    private fun resolveHardwareAddress(
        accountId: String,
        blockchainType: BlockchainType
    ): Pair<HardwarePublicKey, AddressBytesWithPublicKey> {
        val publicKey = runBlocking {
            requireNotNull(
                hardwarePublicKeyStorage.getKeyByBlockchain(accountId, blockchainType)
            )
        }
        val addressWithPublicKey = CustomXPubKeyAddressParser.parse(publicKey.key.value)
        return Pair(publicKey, addressWithPublicKey)
    }

    suspend fun unlink(account: Account) = lifecycleMutex.withLock {
        if (account == currentAccount) {
            useCount.decrementAndGet()

            if (useCount.get() < 1) {
                stopEvmKit()
            }
        }
    }

    suspend fun startForPolling() = lifecycleMutex.withLock {
        pollingSessionCount.onPollingStarted {
            evmKitWrapper?.evmKit?.let { kit ->
                kit.start()
                kit.refresh()
            }
        }
    }

    suspend fun stopForPolling() = lifecycleMutex.withLock {
        pollingSessionCount.onPollingStopped(backgroundManager) {
            evmKitWrapper?.evmKit?.stop()
        }
    }

    private fun subscribeToEvents() {
        job = coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    evmKitWrapper?.evmKit?.let { kit ->
                        Handler(Looper.getMainLooper()).postDelayed({
                            kit.start()
                            kit.refresh()
                        }, 1000)
                    }
                } else if (state == BackgroundManagerState.EnterBackground) {
                    val wrapper = evmKitWrapper ?: return@collect
                    if (pollingSessionCount.get() == 0 && !backgroundKeepAliveManager.isKeepAlive(wrapper.blockchainType)) {
                        wrapper.evmKit.stop()
                    } else {
                        Timber.tag("TxPoller").d("EvmKit(%s) staying alive", wrapper.blockchainType.uid)
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

    fun refresh() {
        evmKitWrapper?.evmKit?.refresh()
    }
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

    suspend fun sendSingle(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long?,
        mevProtectionEnabled: Boolean
    ): FullTransaction {
        if (mevProtectionEnabled && merkleTransactionAdapter == null) {
            throw IllegalStateException("MEV Protection is enabled, but MerkleTransactionAdapter is not initialized")
        }

        val rawTransaction =
            evmKit.rawTransaction(transactionData, gasPrice, gasLimit, nonce).await()
        val (signedRawTransaction, signature) = signReconciled(rawTransaction)
        return if (mevProtectionEnabled && merkleTransactionAdapter != null) {
            merkleTransactionAdapter.send(signedRawTransaction, signature).await()
        } else {
            evmKit.send(signedRawTransaction, signature).await()
        }
    }

    /**
     * Signs [rawTransaction] and returns the transaction that was actually signed together with its
     * signature. Hardware wallets may sign over different fields than requested (Trezor Suite
     * re-estimates the gas limit), so the returned raw transaction can differ from the input. Always
     * go through this instead of the base Signer.signedTransaction(), which signs hardware-wallet
     * transactions with a mock key.
     */
    suspend fun signReconciled(rawTransaction: RawTransaction): Pair<RawTransaction, Signature> {
        val signer = requireNotNull(signer) { "Signer is not initialized for this EVM kit" }
        return when (signer) {
            is TrezorEvmSigner -> signer.signTransaction(rawTransaction)
                .let { it.rawTransaction to it.signature }
            else -> rawTransaction to signer.signature(rawTransaction)
        }
    }

}
