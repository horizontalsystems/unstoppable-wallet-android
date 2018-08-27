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

    var protocolVersion: Int = 0
    var services: Long = 0
    var timestamp: Long = 0

    lateinit var recipientAddress: NetworkAddress
    lateinit var senderAddress: NetworkAddress

    var nonce: Long = 0
    lateinit var subVersion: String

    var lastBlock: Int = 0
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
