package bitcoin.wallet.bitcoin

import android.content.res.AssetManager
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.blockchain.IBlockchainService
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import com.google.common.util.concurrent.MoreExecutors
import io.reactivex.subjects.PublishSubject
import org.bitcoinj.core.*
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.UnreadableWalletException
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


object BitcoinBlockchainService : IBlockchainService {

    var checkpoints: InputStream? = null

    private lateinit var filesDir: File
    private lateinit var storage: BlockchainStorage
    private lateinit var params: NetworkParameters

    private var updateBlockchainHeightSubject = PublishSubject.create<Long>()
    private var updateTransactionsSubject = PublishSubject.create<Map<String, Transaction>>()

    private var latestBlockHeight = 0
        set(value) {
            if (value > field) {
                field = value
                updateBlockchainHeightSubject.onNext(value.toLong())
            }
        }

    private var txs = ConcurrentHashMap<String, Transaction>()
    private lateinit var wallet: Wallet
    private lateinit var spvBlockStore: SPVBlockStore
    private lateinit var peerGroup: PeerGroup

    fun init(dir: File, assetManager: AssetManager, store: BlockchainStorage, testMode: Boolean) {
        BriefLogFormatter.initVerbose()

        filesDir = dir
        storage = store
        params = if (testMode) {
            TestNet3Params.get()
        } else {
            MainNetParams.get()
        }

        checkpoints = assetManager.open("${params.id}.checkpoints.txt")

        val chainFile = File(dir, "${params.paymentProtocolId}.spvchain")
        spvBlockStore = SPVBlockStore(params, chainFile)

        observeSubjects()
    }

    fun initNewWallet() {
        updateBalance(0)
        updateLatestBlockHeight(0)
    }

    fun start(words: List<String>) {
        startWallet(words)
        startPeerGroup()
    }

    override fun getReceiveAddress(): String = wallet.currentReceiveAddress().toBase58()

    override fun sendCoins(address: String, value: Long) = try {
        val targetAddress = Address.fromBase58(params, address)
        val result = wallet.sendCoins(peerGroup, targetAddress, Coin.valueOf(value))
        val transaction = result.broadcastComplete.get()

        transaction.log("Send Coins Transaction")
    } catch (e: InsufficientMoneyException) {
        throw NotEnoughFundsException(e)
    } catch (e: AddressFormatException) {
        throw InvalidAddress(e)
    }

    private fun observeSubjects() {
        updateTransactionsSubject.sample(30, TimeUnit.SECONDS).subscribe {
            dequeueTransactionUpdate()
        }

        updateBlockchainHeightSubject.sample(30, TimeUnit.SECONDS).subscribe {
            storage.updateBlockchainInfo(
                    BlockchainInfo().apply {
                        coinCode = "BTC"
                        latestBlockHeight = it
                    })
        }
    }

    private fun startWallet(words: List<String>) {
        val seedCode = words.joinToString(" ")
        val walletFilename = "${params.paymentProtocolId}-${Integer.toHexString(seedCode.hashCode())}.dat"
        val walletFile = File(filesDir, walletFilename)

        try {
            "Read wallet from file: ${walletFile.absolutePath}".log()
            wallet = WalletBip44.loadFromFile(walletFile, params)
        } catch (e: UnreadableWalletException) {
            "Could not read wallet from file: \"${e.message}\". New wallet will be created".log()
            wallet = WalletBip44.newFromSeedCode(params, seedCode)
            wallet.saveToFile(walletFile)
        }

        wallet.autosaveToFile(walletFile, 0, TimeUnit.SECONDS, null)
        wallet.addCoinsReceivedEventListener { _, tx, prevBalance, newBalance ->
            updateBalance(newBalance.value)
        }

        wallet.addCoinsSentEventListener { _, tx, prevBalance, newBalance ->
            updateBalance(newBalance.value)
        }

        wallet.addTransactionConfidenceEventListener { wallet, tx ->
            enqueueTransactionUpdate(tx)
        }

        if (wallet.lastBlockSeenHeight <= 0) {
            if (checkpoints == null) {
                checkpoints = CheckpointManager.openStream(params)
            }

            CheckpointManager.checkpoint(params, checkpoints, spvBlockStore, wallet.earliestKeyCreationTime)
            wallet.notifyNewBestBlock(spvBlockStore.chainHead)
        }
    }

    private fun startPeerGroup() {
        val spvBlockChain = BlockChain(params, wallet, spvBlockStore)

        spvBlockChain.addNewBestBlockListener {
            updateLatestBlockHeight(it.height)
        }

        peerGroup = PeerGroup(params, spvBlockChain)
        peerGroup.addWallet(wallet)
        peerGroup.addPeerDiscovery(DnsDiscovery(params))
        peerGroup.fastCatchupTimeSecs = wallet.earliestKeyCreationTime
        peerGroup.addConnectedEventListener { peer, peerCount ->
            updateLatestBlockHeight(peerGroup.mostCommonChainHeight)
        }

        peerGroup.addOnTransactionBroadcastListener { _, tx ->
            storage.insertOrUpdateTransactions(listOf(newTransactionRecord(tx)))
        }

        peerGroup.addBlocksDownloadedEventListener { peer, block, filteredBlock, blocksLeft ->
            "Downloaded block: ${block.time}, ${block.hashAsString}, Blocks left: $blocksLeft".log()
        }

        peerGroup.startAsync().addListener(Runnable {
            peerGroup.startBlockChainDownload(DownloadProgressTracker())
        }, MoreExecutors.directExecutor())
    }

    private fun updateLatestBlockHeight(height: Int) {
        latestBlockHeight = height
    }

    private fun updateBalance(balance: Long) {
        storage.updateBalance(Balance().apply {
            code = "BTC"
            value = balance
        })
    }

    private fun enqueueTransactionUpdate(tx: Transaction) {
        synchronized(txs) {
            txs[tx.hashAsString] = tx
            txs.size.log("Transactions count: ")

            updateTransactionsSubject.onNext(txs)
        }
    }

    private fun dequeueTransactionUpdate() {
        synchronized(txs) {
            val transactionRecords = mutableListOf<TransactionRecord>()
            txs.forEach {
                val tx = it.value

                // collect items for bulk write/update
                transactionRecords.add(newTransactionRecord(tx))

                // remove item from queue
                txs.remove(tx.hashAsString)
            }

            storage.insertOrUpdateTransactions(transactionRecords)
        }
    }

    private fun newTransactionRecord(tx: Transaction): TransactionRecord {
        return TransactionRecord().apply {
            transactionHash = tx.hashAsString
            coinCode = "BTC"
            amount = tx.getValue(wallet).value
            incoming = amount > 0
            timestamp = tx.updateTime.time
            blockHeight = try {
                tx.confidence.appearedAtChainHeight.toLong()
            } catch (e: IllegalStateException) {
                0
            }
        }
    }
}
