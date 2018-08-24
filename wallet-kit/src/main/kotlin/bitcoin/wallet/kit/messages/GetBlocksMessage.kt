package bitcoin.wallet.kit.messages

import bitcoin.walllet.kit.common.constant.BitcoinConstants
import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.util.HashUtils
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 * GetBlocks Message
 *
 *  Size       Field       Description
 *  ====       =====       ===========
 *  4 bytes    Version     Negotiated protocol version
 *  VarInt     Count       Number of locator hash entries
 *  Variable   Entries     Locator hash entries
 *  32 bytes   Stop        Hash of the last desired block or zero to get as many as possible
 */
class GetBlocksMessage : Message {

    private var version: Int = 0                    // uint32
    private lateinit var hashes: Array<ByteArray>   // byte[32]
    private lateinit var hashStop: ByteArray        // hash of the last desired block header; set to zero to get as many blocks as possible (2000)

    constructor(firstHash: ByteArray, hashStop: ByteArray) : super("getblocks") {
        version = BitcoinConstants.PROTOCOL_VERSION
        hashes = arrayOf(firstHash)
        this.hashStop = hashStop
    }

    @Throws(IOException::class)
    constructor(payload: ByteArray) : super("getblocks") {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            version = input.readInt()
            val hashCount = input.readVarInt() // do not keep hash count
            hashes = Array(hashCount.toInt()) { input.readBytes(32) }
            hashStop = input.readBytes(32)
        }
    }

    override fun getPayload(): ByteArray {
        val output = BitcoinOutput()
        output.writeInt(version).writeVarInt(hashes.size.toLong())
        for (i in hashes.indices) {
            output.write(hashes[i])
        }
        output.write(hashStop)
        return output.toByteArray()
    }

    override fun toString(): String {
        val list = hashes
                .take(10)
                .map { hash -> HashUtils.toHexStringAsLittleEndian(hash) }
                .joinToString()

        return ("GetBlocksMessage(" + hashes.size + ": [" + list + "], hashStop=" + HashUtils.toHexStringAsLittleEndian(hashStop) + ")")
    }

}