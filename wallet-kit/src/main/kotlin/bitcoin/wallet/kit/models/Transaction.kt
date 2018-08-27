package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.utils.HashUtils
import java.io.IOException

/**
 * Transaction
 *
 *  Size        Field           Description
 *  ====        =====           ===========
 *  4 bytes     Version         Transaction version
 *  VarInt      TxInsCount      Number of inputs
 *  Variable    TxIns           Inputs
 *  VarInt      TxOutsCount     Number of outputs
 *  Variable    TxOuts          Outputs
 *  4 bytes     LockTime        Transaction lock time
 */
class Transaction @Throws(IOException::class) constructor(input: BitcoinInput) {

    /**
     * int32_t, transaction data format version (signed)
     */
    var version: Int = 0

    /**
     * a list of 1 or more transaction inputs or sources for coins
     */
    var txIns: Array<TxIn>

    /**
     * a list of 1 or more transaction outputs or destinations for coins
     */
    var txOuts: Array<TxOut>

    /**
     * uint32_t, the block number or timestamp at which this transaction is
     * unlocked:
     *
     * 0 Not locked
     *
     * < 500000000 Block number at which this transaction is unlocked
     *
     * >= 500000000 UNIX timestamp at which this transaction is unlocked
     *
     * If all TxIn inputs have final (0xffffffff) sequence numbers then
     * lockTime is irrelevant. Otherwise, the transaction may not be added to a
     * block until after lockTime (see NLockTime).
     */
    var lockTime: Long = 0

    /**
     * Get transaction hash (actually calculate the hash of transaction data).
     */
    val txHash: ByteArray by lazy {
        HashUtils.doubleSha256(toByteArray())
    }

    init {
        version = input.readInt()
        val txInCount = input.readVarInt() // do not store count

        txIns = arrayOf()
        for (i in 0 until txInCount.toInt()) {
            txIns[i] = TxIn(input)
        }

        val txOutCount = input.readVarInt() // do not store count
        txOuts = arrayOf()
        for (i in 0 until txOutCount.toInt()) {
            txOuts[i] = TxOut(input)
        }

        lockTime = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        val buffer = BitcoinOutput()
        buffer.writeInt(version).writeVarInt(txIns.size.toLong())
        for (i in txIns.indices) {
            buffer.write(txIns[i].toByteArray())
        }
        buffer.writeVarInt(txOuts.size.toLong())
        for (i in txOuts.indices) {
            buffer.write(txOuts[i].toByteArray())
        }
        buffer.writeUnsignedInt(lockTime)
        return buffer.toByteArray()
    }

    fun getTxInCount(): Long {
        return txIns.size.toLong()
    }

    fun getTxOutCount(): Long {
        return txOuts.size.toLong()
    }

    // http://bitcoin.stackexchange.com/questions/3374/how-to-redeem-a-basic-tx
    fun validate() {
        val output = BitcoinOutput()
        output.writeInt(version)
        // TODO: implement
    }
}
