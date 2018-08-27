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
            this.protocolVersion = input.readInt()
            this.services = input.readLong()
            this.timestamp = input.readLong()
            this.recipientAddress = NetworkAddress.parse(input, true)
            if (this.protocolVersion >= 106) {
                this.senderAddress = NetworkAddress.parse(input, true)
                this.nonce = input.readLong()
                this.subVersion = input.readString()
                this.lastBlock = input.readInt()
                if (this.protocolVersion >= 70001) {
                    this.relay = input.readByte().toInt() != 0
                }
            }
        }
    }

    constructor() : super("version") {}

    constructor(lastBlock: Int, recipientAddr: InetAddress) : super("version") {
        this.protocolVersion = BitcoinConstants.PROTOCOL_VERSION
        this.services = BitcoinConstants.NETWORK_SERVICES
        this.timestamp = System.currentTimeMillis() / 1000
        this.recipientAddress = NetworkAddress(recipientAddr)
        this.senderAddress = NetworkAddress(NetworkUtils.getLocalInetAddress())
        this.nonce = BitcoinConstants.NODE_ID
        this.subVersion = BitcoinConstants.SUB_VERSION
        this.lastBlock = lastBlock
        this.relay = true
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.writeInt(this.protocolVersion) // protocol
                .writeLong(this.services) // services
                .writeLong(timestamp) // timestamp
                .write(this.recipientAddress.toByteArray(true)) // recipient-address
        if (this.protocolVersion >= 106) {
            output.write(this.senderAddress.toByteArray(true)) // sender-address
                    .writeLong(this.nonce) // nodeId
                    .writeString(this.subVersion) // sub-version-string
                    .writeInt(this.lastBlock) // # of last block
            if (this.protocolVersion >= 70001) {
                output.writeByte(1)
            }
        }
        return output.toByteArray()
    }

    override fun toString(): String {
        return ("VersionMessage(lastBlock=" + this.lastBlock + ", protocol=" + this.protocolVersion + ", timestamp="
                + this.timestamp + ")")
    }
}
