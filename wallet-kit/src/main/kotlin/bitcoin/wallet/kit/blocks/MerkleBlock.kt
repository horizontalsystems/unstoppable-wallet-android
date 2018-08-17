package bitcoin.wallet.kit.blocks

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.util.HashUtils
import bitcoin.walllet.kit.struct.Header
import bitcoin.walllet.kit.struct.Transaction

class MerkleBlock(input: BitcoinInput) {

    var header: Header = Header(input)
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
