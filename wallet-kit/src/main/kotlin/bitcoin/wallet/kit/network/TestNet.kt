package bitcoin.wallet.kit.network

open class TestNet : NetworkParameters() {

    override var id: String = ID_TESTNET

    override var port: Int = 18333

    override var packetMagic: Long = 0x0b110907

    override var bip32HeaderPub: Int = 0x043587CF

    override var bip32HeaderPriv: Int = 0x04358394

    override var addressHeader: Int = 111

    override var scriptAddressHeader: Int = 196

    override var coinType: Int = 1

    override var dnsSeeds: Array<String> = arrayOf(
            "testnet-seed.bitcoin.petertodd.org",    // Peter Todd
            "testnet-seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "testnet-seed.bluematt.me",              // Matt Corallo
            "testnet-seed.bitcoin.schildbach.de",    // Andreas Schildbach
            "bitcoin-testnet.bloqseeds.net"         // Bloq
    )

    override var paymentProtocolId: String = PAYMENT_PROTOCOL_ID_TESTNET

}
