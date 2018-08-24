package bitcoin.wallet.kit.messages

import bitcoin.wallet.kit.models.NetworkAddress
import bitcoin.walllet.kit.common.constant.BitcoinConstants
import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.util.NetworkUtils
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

    // How many blocks are in the chain, according to the other side.
    private var lastBlock: Int = 0

    // The version number of the protocol spoken
    private var protocolVersion = BitcoinConstants.PROTOCOL_VERSION

    // Flags defining what optional services are supported.
    private var services = BitcoinConstants.NETWORK_SERVICES

    // What the other side believes the current time to be, in seconds.
    private var timestamp = System.currentTimeMillis() / 1000

    // The network address of the node receiving this message.
    private lateinit var recipientAddress: NetworkAddress

    // The network address of the node emitting this message.
    private lateinit var senderAddress: NetworkAddress

    // Random value to identify sending node
    private var nonce = BitcoinConstants.NODE_ID

    // User-Agent as defined in <a href="https://github.com/bitcoin/bips/blob/master/bip-0014.mediawiki">BIP 14</a>.
    private var subVersion = BitcoinConstants.SUB_VERSION

    // Whether or not to relay tx invs before a filter is received. See <a href="https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#extensions-to-existing-messages">BIP 37</a>.
    private var relay = true

    constructor(bestBlock: Int, recipientAddr: InetAddress) : super("version") {
        lastBlock = bestBlock
        recipientAddress = NetworkAddress(recipientAddr)
        senderAddress = NetworkAddress(NetworkUtils.getLocalInetAddress())
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
        output.writeInt(protocolVersion)                               // protocol
                .writeLong(services)                                   // services
                .writeLong(timestamp)                                       // timestamp
                .write(recipientAddress.toByteArray(true))  // recipient-address
        if (protocolVersion >= 106) {
            output.write(senderAddress.toByteArray(true))   // sender-address
                    .writeLong(nonce)                                  // nodeId
                    .writeString(subVersion)                           // sub-version-string
                    .writeInt(lastBlock)                               // hash of last block
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
