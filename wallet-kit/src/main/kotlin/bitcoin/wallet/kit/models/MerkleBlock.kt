package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.HashUtils

/**
 * MerkleBlock
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  80 bytes    Header          Consists of 6 fields that are hashed to calculate the block hash
 *  VarInt      HashCount       Number of hashes
 *  Variable    Hashes          Hashes in depth-first order
 *  VarInt      FlagsCount      Number of bytes of flag bits
 *  Variable    Flags           Flag bits packed 8 per byte, least significant bit first
 */
class MerkleBlock(input: BitcoinInput) {

    var header = Header(input)
    var hashes: Array<ByteArray> = arrayOf()
    var flags: ByteArray = byteArrayOf()

    var associatedTransactionHashes: Array<ByteArray> = arrayOf()
    val associatedTransactions: MutableList<Transaction> = mutableListOf()
    val blockHash: ByteArray by lazy {
        HashUtils.doubleSha256(header.toByteArray())
    }

    private var txnCount: Int = 0
    private var hashCount: Long = 0L
    private var flagsCount: Long

    init {
        txnCount = input.readInt()
        hashCount = input.readVarInt()

        hashes = arrayOf()
        for (i in 0 until hashCount) {
            hashes[i.toInt()] = input.readBytes(32)
        }

        flagsCount = input.readVarInt()
        flags = input.readBytes(flagsCount.toInt())
    }


    fun toByteArray(): ByteArray {
        val output = BitcoinOutput()
        output.write(header.toByteArray())
        output.writeInt(txnCount)
        output.writeVarInt(hashCount)
        output.writeVarInt(flagsCount)
        output.write(flags)

        return output.toByteArray()
    }

    fun addTransaction(transaction: Transaction) {
        associatedTransactions.add(transaction)
    }

}
