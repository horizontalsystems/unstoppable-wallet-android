package bitcoin.wallet.bitcoin

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.entities.BlockchainInfo
import bitcoin.wallet.entities.TransactionRecord
import bitcoin.wallet.log
import com.google.common.collect.ImmutableList
import org.bitcoinj.core.BlockChain
import org.bitcoinj.core.PeerGroup
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.TestNet3Params
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.utils.BriefLogFormatter
import org.bitcoinj.wallet.DeterministicKeyChain
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroup
import org.bitcoinj.wallet.Wallet
import java.io.File


object BitcoinBlockchainService {

    fun start(storage: BlockchainStorage, chainFileDir: File) {

        BriefLogFormatter.initVerbose()

        val params = TestNet3Params()

        val seedCode = "used ugly meat glad balance divorce inner artwork hire invest already piano"
        val creationTime = 1525845045L
        val seed = DeterministicSeed(seedCode, null, "", creationTime)

        val keyChain = Bip44TestNetDeterministicKeyChain(seed)
        val keyChainGroup = KeyChainGroup(params)
        keyChainGroup.addAndActivateHDChain(keyChain)

        val wallet = Wallet(params, keyChainGroup)
        val chainFile = File(chainFileDir, "${params.paymentProtocolId}.spvchain")
        val spvBlockStore = SPVBlockStore(params, chainFile)
        val spvBlockChain = BlockChain(params, wallet, spvBlockStore)

        wallet.addChangeEventListener {
            it.balance.log("Balance: ")

        }

        wallet.addTransactionConfidenceEventListener { wallet, tx ->
            insertOrUpdateTransaction(storage, tx, wallet)
        }

        val peerGroup = PeerGroup(params, spvBlockChain)
        peerGroup.addWallet(wallet)
        peerGroup.addPeerDiscovery(DnsDiscovery(params))
        peerGroup.addBlocksDownloadedEventListener { peer, block, filteredBlock, blocksLeft ->
            "Downloaded block: ${block.hashAsString}, Blocks left: $blocksLeft".log()
        }

        peerGroup.addOnTransactionBroadcastListener { peer, t ->
            insertOrUpdateTransaction(storage, t, wallet)
        }

        spvBlockChain.addNewBestBlockListener {
            val blockchainInfo = BlockchainInfo().apply {
                coinCode = "BTC"
                latestBlockHeight = it.height.toLong()
            }

            "New best block height: ${it.height}".log()

//            storage.updateBlockchainInfo(blockchainInfo)
        }

        peerGroup.startAsync()
    }

    private fun insertOrUpdateTransaction(storage: BlockchainStorage, t: Transaction, wallet: Wallet) {
        val transactionRecord = TransactionRecord().apply {
            transactionHash = t.hashAsString
            coinCode = "BTC"
            amount = t.getValue(wallet).value
            incoming = true
            timestamp = t.updateTime.time

            try {
                blockHeight = t.confidence.appearedAtChainHeight.toLong()
            } catch (e: IllegalStateException) {
                blockHeight = 0
            }
        }

        storage.insertOrUpdateTransaction(transactionRecord)
    }

}

class Bip44TestNetDeterministicKeyChain(seed: DeterministicSeed) : DeterministicKeyChain(seed) {

    override fun getAccountPath(): ImmutableList<ChildNumber> =
            ImmutableList.of(ChildNumber(44, true), ChildNumber(1, true), ChildNumber.ZERO_HARDENED)

}

class Bip44DeterministicKeyChain(seed: DeterministicSeed) : DeterministicKeyChain(seed) {

    override fun getAccountPath(): ImmutableList<ChildNumber> = BIP44_ACCOUNT_ZERO_PATH

}
