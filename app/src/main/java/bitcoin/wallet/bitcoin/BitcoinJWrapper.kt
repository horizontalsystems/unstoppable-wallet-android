package bitcoin.wallet.bitcoin

import android.content.res.AssetManager
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

    lateinit var wallet: Wallet
    lateinit var spvBlockChain: BlockChain
    lateinit var peerGroup: PeerGroup

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

    fun prepareEnvForWallet(words: List<String>) {
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

    }

    fun startAsync() {
        peerGroup.startAsync().addListener(Runnable {
            peerGroup.startBlockChainDownload(DownloadProgressTracker())
        }, MoreExecutors.directExecutor())
    }

    fun sendCoins(address: String, value: Long) {
        val targetAddress = Address.fromBase58(params, address)
        val result = wallet.sendCoins(peerGroup, targetAddress, Coin.valueOf(value))
        val transaction = result.broadcastComplete.get()

        transaction.log("Send Coins Transaction")
    }

    fun getReceiveAddress(): String = wallet.currentReceiveAddress().toBase58()


}
