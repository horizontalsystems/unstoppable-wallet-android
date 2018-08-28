package bitcoin.wallet.kit

import bitcoin.wallet.kit.blocks.BlockSyncer
import bitcoin.wallet.kit.crypto.BloomFilter
import bitcoin.wallet.kit.hdwallet.HDWallet
import bitcoin.wallet.kit.hdwallet.Mnemonic
import bitcoin.wallet.kit.hdwallet.PublicKey
import bitcoin.wallet.kit.headers.HeaderSyncer
import bitcoin.wallet.kit.managers.Syncer
import bitcoin.wallet.kit.network.MainNet
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.network.PeerManager

class Wallet {
    private val mnemonic = Mnemonic()
    private var keys = listOf("used", "ugly", "meat", "glad", "balance", "divorce", "inner", "artwork", "hire", "invest", "already", "piano")
    private var seed = mnemonic.toSeed(keys)
    private var wall = HDWallet(seed, MainNet())
    private val pubKeys: MutableList<PublicKey> = mutableListOf()

    init {
        for (i in 1..10) {
            pubKeys.add(wall.changeAddress(i))
            pubKeys.add(wall.receiveAddress(i))
        }
    }

    fun pubKeys(): MutableList<PublicKey> {
        return pubKeys
    }
}

class WalletKit {
    private var peerGroup: PeerGroup

    init {
        val wallet = Wallet()
        val pubKeys = wallet.pubKeys()
        val filters = BloomFilter(pubKeys.size)

        pubKeys.forEach {
            filters.insert(it.publicKey)
        }

        val peerManager = PeerManager()

        peerGroup = PeerGroup(peerManager, 1)
        peerGroup.setBloomFilter(filters)
        peerGroup.listener = Syncer(
                peerGroup,
                HeaderSyncer(peerGroup),
                BlockSyncer(peerGroup)
        )
        peerGroup.start()
    }
}

fun main(args: Array<String>) {
    WalletKit()
}
