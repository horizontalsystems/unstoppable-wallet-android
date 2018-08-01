package bitcoin.wallet.bitcoin

import android.content.res.AssetManager
import bitcoin.wallet.blockchain.InvalidAddress
import bitcoin.wallet.blockchain.NotEnoughFundsException
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import com.google.common.util.concurrent.MoreExecutors
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
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

class BitcoinJWrapper(private val filesDir: File, assetManager: AssetManager, testMode: Boolean) {

    private val params: NetworkParameters
    private val checkpoints: InputStream

    private lateinit var wallet: Wallet
    private lateinit var spvBlockChain: BlockChain
    private lateinit var peerGroup: PeerGroup

    init {
        BriefLogFormatter.initVerbose()

        params = if (testMode) {
            TestNet3Params.get()
        } else {
            MainNetParams.get()
        }

        checkpoints = try {
            assetManager.open("${params.id}.checkpoints.txt")
        } catch (e: IOException) {
            CheckpointManager.openStream(params)
        }
    }

    fun prepareEnvForWallet(words: List<String>, listener: BitcoinChangeListener) {
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

        val spvBlockStore = SPVBlockStore(params, File(filesDir, "${params.paymentProtocolId}.spvchain"))
        if (wallet.lastBlockSeenHeight <= 0) {
            CheckpointManager.checkpoint(params, checkpoints, spvBlockStore, wallet.earliestKeyCreationTime)
            wallet.notifyNewBestBlock(spvBlockStore.chainHead)
        }

        spvBlockChain = BlockChain(params, wallet, spvBlockStore)

        peerGroup = PeerGroup(params, spvBlockChain)
        peerGroup.addWallet(wallet)
        peerGroup.addPeerDiscovery(DnsDiscovery(params))
        if (wallet.lastBlockSeenHeight <= 0) {
            peerGroup.fastCatchupTimeSecs = wallet.earliestKeyCreationTime
        }

        peerGroup.addBlocksDownloadedEventListener { peer, block, filteredBlock, blocksLeft ->
            "Downloaded block: ${block.time}, ${block.hashAsString}, Blocks left: $blocksLeft".log()
        }

        val transactionConfidenceTypes = wallet.getTransactions(true).map { it.hashAsString to it.confidence.confidenceType }.toMap().toMutableMap()
        var balance = wallet.balance
        var latestBlockHeight = spvBlockChain.bestChainHeight

        wallet.addChangeEventListener { changedWallet ->
            changedWallet.getTransactions(true).forEach { txNewState ->

                val prevConfidenceType = transactionConfidenceTypes[txNewState.hashAsString]
                val newConfidenceType = txNewState.confidence.confidenceType

                if (prevConfidenceType != newConfidenceType) {
                    if (balance != changedWallet.balance) {
                        listener.onBalanceChange(changedWallet.balance.value)
                        balance = changedWallet.balance
                    }

                    if (prevConfidenceType == null) {
                        listener.onNewTransaction(newTransactionRecord(txNewState))
                    } else {
                        listener.onTransactionConfidenceChange(newTransactionRecord(txNewState))
                    }

                    transactionConfidenceTypes[txNewState.hashAsString] = newConfidenceType
                }
            }
        }

        spvBlockChain.addNewBestBlockListener {
            if (it.height > latestBlockHeight) {
                latestBlockHeight = it.height
                listener.onBestChainHeightChange(latestBlockHeight)
            }
        }

        peerGroup.addConnectedEventListener { peer, peerCount ->
            if (peerGroup.mostCommonChainHeight > latestBlockHeight) {
                latestBlockHeight = peerGroup.mostCommonChainHeight
                listener.onBestChainHeightChange(latestBlockHeight)
            }
        }
    }

    fun startAsync(tracker: DownloadProgressTracker) {
        peerGroup.startAsync().addListener(Runnable {
            peerGroup.startBlockChainDownload(tracker)
        }, MoreExecutors.directExecutor())
    }

    fun sendCoins(address: String, value: Long) = try {
        val targetAddress = Address.fromBase58(params, address)
        val result = wallet.sendCoins(peerGroup, targetAddress, Coin.valueOf(value))
        val transaction = result.broadcastComplete.get()

        transaction.log("Send Coins Transaction")
    } catch (e: InsufficientMoneyException) {
        throw NotEnoughFundsException(e)
    } catch (e: AddressFormatException) {
        throw InvalidAddress(e)
    }

    fun getReceiveAddress(): String = wallet.currentReceiveAddress().toBase58()

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
