package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.hdwallet.HDKey

class PublicKey() {

    var index = 0
    var external = true
    var address = ""

    lateinit var network: NetworkParameters
    lateinit var publicKey: ByteArray
    lateinit var publicKeyHash: ByteArray

    constructor(index: Int, external: Boolean, key: HDKey, network: NetworkParameters) : this() {
        this.index = index
        this.network = network
        this.external = external
        this.publicKey = key.pubKey
        this.publicKeyHash = key.pubKeyHash
        this.address = key.toAddress(network).toString()
    }
}
