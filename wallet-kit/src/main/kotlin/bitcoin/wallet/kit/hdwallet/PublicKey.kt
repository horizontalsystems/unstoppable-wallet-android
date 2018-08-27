package bitcoin.wallet.kit.hdwallet

import bitcoin.walllet.kit.hdwallet.HDKey

class PublicKey() {

    var index = 0
    var external = true
    var address = ""

    lateinit var publicKey: ByteArray
    lateinit var publicKeyHash: ByteArray

    constructor(index: Int, external: Boolean, key: HDKey) : this() {
        this.index = index
        this.external = external
        this.publicKey = key.pubKey
        this.publicKeyHash = key.pubKeyHash
        this.address = key.toAddress().toString()
    }
}
