package bitcoin.wallet.kit.hdwallet

import bitcoin.walllet.kit.common.hdwallet.HDKey

class PublicKey() {

    var index = 0
    var external = true
    var publicKey: ByteArray? = null
    var publicKeyHash: ByteArray? = null
    var address = ""

    constructor(index: Int, external: Boolean, key: HDKey): this() {
        this.index = index
        this.external = external
        this.publicKey = key.pubKey
        this.publicKeyHash = key.pubKeyHash
        this.address = key.toAddress().toString()
    }
}
