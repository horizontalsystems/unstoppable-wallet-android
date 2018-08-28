package bitcoin.wallet.kit.network

class MainNet : NetworkParameters() {

    override var id: String = ID_MAINNET

    override var port: Int = 8333

    override var packetMagic: Long = 0xf9beb4d9L

    override var bip32HeaderPub: Int = 0x0488B21E //The 4 byte header that serializes in base58 to "xpub".

    override var bip32HeaderPriv: Int = 0x0488ADE4 //The 4 byte header that serializes in base58 to "xprv"

    override var addressHeader: Int = 0

    override var scriptAddressHeader: Int = 5

    override var coinType: Int = 0

    override var dnsSeeds: Array<String> = arrayOf(
            "seed.bitcoin.sipa.be", // Pieter Wuille
            "dnsseed.bluematt.me", // Matt Corallo
            "dnsseed.bitcoin.dashjr.org", // Luke Dashjr
            "seed.bitcoinstats.com", // Chris Decker
            "seed.bitnodes.io", // Addy Yeow
            "bitseed.xf2.org", // Jeff Garzik
            "seed.bitcoin.jonasschnelli.ch", // Jonas Schnelli
            "bitcoin.bloqseeds.net"// Bloq
    )

    override var paymentProtocolId: String = PAYMENT_PROTOCOL_ID_MAINNET

}
