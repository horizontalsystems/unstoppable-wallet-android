package bitcoin.wallet.bitcoin

import android.content.res.AssetManager
import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.Balance
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.ReceiveAddress
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import io.reactivex.subjects.PublishSubject
import org.bitcoinj.core.*
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.UnreadableWalletException
import org.bitcoinj.wallet.Wallet
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit


object BitcoinBlockchainService {

    var seedCode: String = ""
    var checkpoints: InputStream? = null

    lateinit var filesDir: File
    lateinit var storage: BlockchainStorage
    private lateinit var params: NetworkParameters

    private var updateBlockchainHeightSubject = PublishSubject.create<Long>()
    private var transactionsUpdatedSubject = PublishSubject.create<Map<String, Transaction>>()

    private var latestBlockHeight = 0
        set(value) {
            if (value > field) {
                field = value
                updateBlockchainHeightSubject.onNext(value.toLong())
            }
        }

    private var txs = mutableMapOf<String, Transaction>()
    private lateinit var wallet: Wallet

    fun init(filesDir: File, assetManager: AssetManager, storage: BlockchainStorage, testMode: Boolean) {
        BriefLogFormatter.initVerbose()

        this.filesDir = filesDir
        this.storage = storage

        params = if (testMode) {
            TestNet3Params.get()
        } else {
            MainNetParams.get()

        }

        checkpoints = assetManager.open("${params.id}.checkpoints.txt")

        updateBlockchainHeightSubject.sample(30, TimeUnit.SECONDS).subscribe {
            val blockchainInfo = BlockchainInfo().apply {
                coinCode = "BTC"
                latestBlockHeight = it
            }

            storage.updateBlockchainInfo(blockchainInfo)
        }

        transactionsUpdatedSubject.sample(30, TimeUnit.SECONDS).subscribe { txs ->

            val transactionRecords = mutableListOf<TransactionRecord>()
            txs.forEach {
                val t = it.value
                val transactionRecord = TransactionRecord().apply {
                    transactionHash = t.hashAsString
                    coinCode = "BTC"
                    amount = t.getValue(wallet).value
                    incoming = amount > 0
                    timestamp = t.updateTime.time

                    blockHeight = try {
                        t.confidence.appearedAtChainHeight.toLong()
                    } catch (e: IllegalStateException) {
                        0
                    }
                }

                transactionRecords.add(transactionRecord)

            }

            "Updating transactions ${transactionRecords.joinToString { it.transactionHash }}".log()

            storage.insertOrUpdateTransactions(transactionRecords)

            this.txs.clear()

            "Cleared transactions, tx count: ${txs.count()}".log()
        }

    }

    fun initNewWallet() {
        updateBalance(0)
        updateLatestBlockHeight(0)
    }


    fun start() {
        seedCode = Factory.preferencesManager.savedWords?.joinToString(" ") ?: throw Exception("No saved words")

        val chainFile = File(filesDir, "${params.paymentProtocolId}.spvchain")
        val spvBlockStore = SPVBlockStore(params, chainFile)

        wallet = getWallet()

        updateReceiveAddress(wallet.currentReceiveAddress().toBase58())

        if (wallet.lastBlockSeenHeight <= 0) {
            if (checkpoints == null) {
                checkpoints = CheckpointManager.openStream(params)
            }

            CheckpointManager.checkpoint(params, checkpoints, spvBlockStore, wallet.earliestKeyCreationTime)
            wallet.notifyNewBestBlock(spvBlockStore.chainHead)
        }

        val spvBlockChain = BlockChain(params, wallet, spvBlockStore)

        wallet.addCoinsReceivedEventListener { _, tx, prevBalance, newBalance ->

            updateReceiveAddress(wallet.currentReceiveAddress().toBase58())

            updateBalance(newBalance.value)
        }
        wallet.addCoinsSentEventListener { _, tx, prevBalance, newBalance ->
            updateBalance(newBalance.value)
        }
        wallet.addTransactionConfidenceEventListener { wallet, tx ->
            updateTransaction(tx)
        }

        val peerGroup = PeerGroup(params, spvBlockChain)

        peerGroup.addWallet(wallet)
        peerGroup.addPeerDiscovery(DnsDiscovery(params))
        peerGroup.fastCatchupTimeSecs = wallet.earliestKeyCreationTime

        peerGroup.addConnectedEventListener { peer, peerCount ->
            updateLatestBlockHeight(peerGroup.mostCommonChainHeight)
        }
        peerGroup.addOnTransactionBroadcastListener { peer, t ->
            updateTransaction(t)
        }
        spvBlockChain.addNewBestBlockListener {
            updateLatestBlockHeight(it.height)
        }

        peerGroup.addBlocksDownloadedEventListener { peer, block, filteredBlock, blocksLeft ->
            "Downloaded block: ${block.time}, ${block.hashAsString}, Blocks left: $blocksLeft".log()
        }

        peerGroup.startAsync()
    }

    private fun getWallet(): Wallet {
        val walletFilename = "${params.paymentProtocolId}-${Integer.toHexString(seedCode.hashCode())}.dat"
        val walletFile = File(filesDir, walletFilename)

        var wallet: Wallet
        try {
            "Read wallet from file: ${walletFile.absolutePath}".log()
            wallet = WalletBip44.loadFromFile(walletFile, params)
        } catch (e: UnreadableWalletException) {
            "Could not read wallet from file: \"${e.message}\". New wallet will be created".log()
            wallet = WalletBip44.newFromSeedCode(params, seedCode)
            wallet.saveToFile(walletFile)
        }

        wallet.autosaveToFile(walletFile, 0, TimeUnit.SECONDS, null)

        return wallet
    }

    private fun updateLatestBlockHeight(v: Int) {
        latestBlockHeight = v
    }

    private fun updateBalance(v: Long) {
        storage.updateBalance(Balance().apply {
            code = "BTC"
            value = v
        })
    }

    private fun updateReceiveAddress(address: String) {
        address.log("Updating receive address: ")

        storage.updateReceiveAddress(ReceiveAddress().apply {
            code = "BTC"
            this.address = address
        })
    }

    private fun updateTransaction(t: Transaction) {
        txs[t.hashAsString] = t

        txs.size.log("Transactions count: ")

        transactionsUpdatedSubject.onNext(txs)

    }

}
