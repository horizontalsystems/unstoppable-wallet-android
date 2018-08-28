package bitcoin.wallet.kit.network


/**
 * <p>NetworkParameters contains the data needed for working with an instantiation of a Bitcoin chain.</p>
 */

abstract class NetworkParameters{

    /** The string returned by getId() for the main, production network where people trade things.  */
    val ID_MAINNET = "org.bitcoin.production"
    /** The string returned by getId() for the testnet.  */
    val ID_TESTNET = "org.bitcoin.test"
    /** The string returned by getId() for regtest mode.  */
    val ID_REGTEST = "org.bitcoin.regtest"

    /** The string used by the payment protocol to represent the main net.  */
    val PAYMENT_PROTOCOL_ID_MAINNET = "main"
    /** The string used by the payment protocol to represent the test net.  */
    val PAYMENT_PROTOCOL_ID_TESTNET = "test"
    /** The string used by the payment protocol to represent the regtest net.  */
    val PAYMENT_PROTOCOL_ID_REGTEST = "regtest"

    abstract var id: String

     abstract var port: Int

    // Indicates message origin network and is used to seek to the next message when stream state is unknown.
     abstract var packetMagic: Long

     abstract var bip32HeaderPub: Int
     abstract var bip32HeaderPriv: Int
     abstract var addressHeader: Int
     abstract var scriptAddressHeader: Int
     abstract var coinType: Int

     abstract var dnsSeeds: Array<String>

     abstract var paymentProtocolId: String

    fun isMainNet(): Boolean {
        return id == ID_MAINNET
    }

}
