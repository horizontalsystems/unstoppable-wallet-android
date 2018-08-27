package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.serializer.HashSerializer
import bitcoin.walllet.kit.serializer.TimestampSerializer
import bitcoin.walllet.kit.utils.HashUtils
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

/**
 * Header
 *
 *   Size       Field           Description
 *   ====       =====           ===========
 *   4 bytes    Version         The block version number
 *   32 bytes   PrevHash        The hash of the preceding block in the chain
 *   32 byte    MerkleHash      The Merkle root for the transactions in the block
 *   4 bytes    Time            The time the block was mined
 *   4 bytes    Bits            The target difficulty
 *   4 bytes    Nonce           The nonce used to generate the required hash
 */
class Header @Throws(IOException::class) constructor(input: BitcoinInput) {

    // Int32, block version information (note, this is signed)
    var version: Int = 0

    // The hash value of the previous block this particular block references
    @JsonSerialize(using = HashSerializer::class)
    var prevHash: ByteArray

    // The reference to a Merkle tree collection which is a hash of all transactions related to this block
    @JsonSerialize(using = HashSerializer::class)
    var merkleHash: ByteArray

    // Uint32, A timestamp recording when this block was created (Will overflow in 2106)
    @JsonSerialize(using = TimestampSerializer::class)
    var timestamp: Long = 0

    // Uint32, The calculated difficulty target being used for this block
    var bits: Long = 0

    // Uint32, The nonce used to generate this block to allow variations of the header and compute different hashes
    var nonce: Long = 0

    init {
        version = input.readInt()
        prevHash = input.readBytes(32)
        merkleHash = input.readBytes(32)
        timestamp = input.readUnsignedInt()
        bits = input.readUnsignedInt()
        nonce = input.readUnsignedInt()
    }

    val hash: ByteArray by lazy {
        HashUtils.doubleSha256(toByteArray())
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .writeInt(version)
                .write(prevHash)
                .write(merkleHash)
                .writeUnsignedInt(timestamp)
                .writeUnsignedInt(bits)
                .writeUnsignedInt(nonce)
                .toByteArray()
    }
}
