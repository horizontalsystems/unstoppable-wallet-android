package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.BytesUtils
import bitcoin.walllet.kit.utils.HashUtils
import java.io.IOException
import java.util.*

/**
 * Block
 *
 *   Size       Field       Description
 *   ====       =====       ===========
 *   80 bytes   Header      Consists of 6 fields that are hashed to calculate the block hash
 *   VarInt     TxCount     Number of transactions in the block
 *   Variable   Txns        The transactions in the block
 */
class Block @Throws(IOException::class) constructor(input: BitcoinInput) {

    var header: Header
    var txns: Array<Transaction>

    init {
        header = Header(input)
        val txnCount = input.readVarInt() // do not store txn_count

        txns = Array(txnCount.toInt()) {
            Transaction(input)
        }
    }

    val blockHash: ByteArray by lazy {
        header.hash
    }

    fun calculateMerkleHash(): ByteArray {
        var hashes = Arrays.asList(*txns)
                .map { tx -> tx.txHash }
                .toTypedArray()

        while (hashes.size > 1) {
            hashes = merkleHash(hashes)
        }

        return hashes[0]
    }

    private fun merkleHash(hashes: Array<ByteArray>): Array<ByteArray> {
        val count = hashes.size / 2
        val extra = hashes.size % 2

        val results = Array(count + extra) { i ->
            HashUtils.doubleSha256(BytesUtils.concat(hashes[2 * i], hashes[2 * i + 1]))
        }

        if (extra == 1) {
            results[count] = HashUtils.doubleSha256(BytesUtils.concat(hashes[hashes.size - 1], hashes[hashes.size - 1]))
        }

        return results
    }

    fun toByteArray(): ByteArray {
        val output = BitcoinOutput()
        output.write(header.toByteArray())
        output.writeVarInt(txns.size.toLong())
        for (tx in txns) {
            output.write(tx.toByteArray())
        }

        return output.toByteArray()
    }
}
