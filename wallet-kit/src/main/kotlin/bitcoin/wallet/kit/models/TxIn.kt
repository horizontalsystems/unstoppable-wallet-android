package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.serializer.HexSerializer
import bitcoin.walllet.kit.common.util.BytesUtils
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

/**
 * Transaction input
 *
 *  Size        Field               Description
 *  ===         =====               ===========
 *  32 bytes    TxOutHash           Double SHA-256 hash of the transaction containing the output to be used by this input
 *  4 bytes     TxOutIndex          Index of the output within the transaction
 *  VarInt      TxInScriptLength    Script length
 *  Variable    TxInScript          Script
 *  4 bytes     TxInSeqNumber       Input sequence number (irrelevant unless transaction LockTime is non-zero)
 */
class TxIn @Throws(IOException::class) constructor(input: BitcoinInput) {

    var previousOutput = OutPoint(input)

    @JsonSerialize(using = HexSerializer::class)
    var sigScript: ByteArray

    /**
     * uint32, Transaction version as defined by the sender. Intended for
     * "replacement" of transactions when information is updated before
     * inclusion into a block.
     */
    var sequence: Long = 0

    val isCoinbase: Boolean
        get() = (previousOutput.hash != null && BytesUtils.isZeros(previousOutput.hash))

    init {
        val sigScriptLength = input.readVarInt()
        sigScript = input.readBytes(sigScriptLength.toInt())
        sequence = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(previousOutput.toByteArray())
                .writeVarInt(sigScript.size.toLong())
                .write(sigScript)
                .writeUnsignedInt(sequence)
                .toByteArray()
    }

    fun validate() {
        // throw new ValidateException("Verify signature failed.");
    }
}
