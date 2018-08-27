package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import bitcoin.walllet.kit.serializer.SatoshiSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

/**
 * Transaction output
 *
 *  Size        Field               Description
 *  ====        =====               ===========
 *  8 bytes     TxOutValue          Value expressed in Satoshis (0.00000001 BTC)
 *  VarInt      TxOutScriptLength   Script length
 *  Variable    TxOutScript         Script
 */
class TxOut @Throws(IOException::class) constructor(input: BitcoinInput) {

    /**
     * int64, Transaction Value
     */
    @JsonSerialize(using = SatoshiSerializer::class)
    var value: Long = 0

    /**
     * uchar[], Usually contains the public key as a Bitcoin script setting up
     * conditions to claim this output.
     */
    var pkScript: ByteArray

    init {
        value = input.readLong()
        val scriptLength = input.readVarInt()
        pkScript = input.readBytes(scriptLength.toInt())
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .writeLong(value)
                .writeVarInt(pkScript.size.toLong())
                .write(pkScript)
                .toByteArray()
    }
}
