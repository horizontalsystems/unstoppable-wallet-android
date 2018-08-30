package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.NetworkAddress
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.NetworkUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress

/**
 * Version Message
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Version         Protocol version
 *   8 bytes    Services        Supported services (bit field)
 *   8 bytes    Timestamp       Time in seconds since the epoch
 *  26 bytes    RemoteAddress   Remote node address
 *  26 bytes    LocalAddress    Local node address
 *   8 bytes    Nonce           Random value to identify sending node
 * VarString    UserAgent       Identification string
 *   4 bytes    BlockHeight     Last block received by sending node
 *   1 byte     TxRelay         TRUE if remote peer should relay transactions
 */
class VersionMessage : Message {

    // The version number of the protocol spoken
    private var protocolVersion: Int = 0

    // Flags defining what optional services are supported.
    private var services: Long = 0L

    // What the other side believes the current time to be, in seconds.
    private var timestamp = System.currentTimeMillis() / 1000

    // The network address of the node receiving this message.
    private lateinit var recipientAddress: NetworkAddress

    // The network address of the node emitting this message.
    private lateinit var senderAddress: NetworkAddress

    // Random value to identify sending node
    private var nonce = 0L

    // User-Agent as defined in <a href="https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki">BIP 14</a>.
    private var subVersion = "/Satoshi:0.7.2/"

    // How many blocks are in the chain, according to the other side.
    var lastBlock: Int = 0

    // Whether or not to relay tx invs before a filter is received.
    // See <a href="https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages">BIP 37</a>.
    private var relay = false

    constructor(bestBlock: Int, recipientAddr: InetAddress, network: NetworkParameters) : super("version") {
        protocolVersion = network.protocolVersion
        services = network.networkServices
        lastBlock = bestBlock
        recipientAddress = NetworkAddress(recipientAddr, network)
        senderAddress = NetworkAddress(NetworkUtils.getLocalInetAddress(), network)
        nonce = (Math.random() * java.lang.Long.MAX_VALUE).toLong() //Random node id generated at startup.
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("version") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            protocolVersion = input.readInt()
            services = input.readLong()
            timestamp = input.readLong()
            recipientAddress = NetworkAddress.parse(input, true)
            if (protocolVersion >= 106) {
                senderAddress = NetworkAddress.parse(input, true)
                nonce = input.readLong()
                subVersion = input.readString()
                lastBlock = input.readInt()
                if (protocolVersion >= 70001) {
                    relay = input.readByte().toInt() != 0
                }
            }
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.writeInt(protocolVersion) // protocol
                .writeLong(services) // services
                .writeLong(timestamp) // timestamp
                .write(recipientAddress.toByteArray(true)) // recipient-address
        if (protocolVersion >= 106) {
            output.write(senderAddress.toByteArray(true)) // sender-address
                    .writeLong(nonce) // nodeId
                    .writeString(subVersion) // sub-version-string
                    .writeInt(lastBlock) // # of last block
            if (protocolVersion >= 70001) {
                output.writeByte(1)
            }
        }
        return output.toByteArray()
    }

    fun hasBlockChain(network: NetworkParameters): Boolean {
        return (services and network.serviceFullNode) == network.serviceFullNode
    }

    fun supportsBloomFilter(network: NetworkParameters): Boolean {
        return protocolVersion >= network.bloomFilter
    }

    override fun toString(): String {
        return ("VersionMessage(lastBlock=$lastBlock, protocol=$protocolVersion, services=$services, timestamp=$timestamp)")
    }
}
