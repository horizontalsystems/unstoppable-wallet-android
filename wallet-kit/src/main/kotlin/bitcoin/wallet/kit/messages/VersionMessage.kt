package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.NetworkAddress
import bitcoin.walllet.kit.constant.BitcoinConstants
import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.NetworkUtils
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress


/**
 * <p>Version Message</p>
 * <pre>
 *   Size       Field               Description
 *   ====       =====               ===========
 *   4 bytes    Version             Protocol version
 *   8 bytes    Services            Supported services (bit field)
 *   8 bytes    Timestamp           Time in seconds since the epoch
 *  26 bytes    RemoteAddress       Remote node address
 *  26 bytes    LocalAddress        Local node address
 *   8 bytes    Nonce               Random value to identify sending node
 *  VarString   UserAgent           Identification string
 *   4 bytes    BlockHeight         Last block received by sending node
 *   1 byte     TxRelay             TRUE if remote peer should relay transactions
 * </pre>
 */


class VersionMessage : Message {

    // The version number of the protocol spoken
    var protocolVersion: Int = 0

    // Flags defining what optional services are supported.
    var services: Long = 0

    // What the other side believes the current time to be, in seconds.
    var timestamp: Long = 0

    // The network address of the node receiving this message.
    lateinit var recipientAddress: NetworkAddress

    // The network address of the node emitting this message.
    lateinit var senderAddress: NetworkAddress

    // Random value to identify sending node
    var nonce: Long = 0

    // User-Agent as defined in <a href="https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki">BIP 14</a>.
    lateinit var subVersion: String

    // How many blocks are in the chain, according to the other side.
    var lastBlock: Int = 0

    // Whether or not to relay tx invs before a filter is received.
    // See <a href="https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages">BIP 37</a>.
    var relay: Boolean = false

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

    constructor() : super("version") {}

    constructor(lastBlock: Int, recipientAddr: InetAddress) : super("version") {
        protocolVersion = BitcoinConstants.PROTOCOL_VERSION
        services = BitcoinConstants.NETWORK_SERVICES
        timestamp = System.currentTimeMillis() / 1000
        recipientAddress = NetworkAddress(recipientAddr)
        senderAddress = NetworkAddress(NetworkUtils.getLocalInetAddress())
        nonce = BitcoinConstants.NODE_ID
        subVersion = BitcoinConstants.SUB_VERSION
        this.lastBlock = lastBlock
        relay = true
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

    override fun toString(): String {
        return ("VersionMessage(lastBlock=$lastBlock, protocol=$protocolVersion, timestamp=$timestamp)")
    }
}
