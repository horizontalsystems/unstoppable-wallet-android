package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.common.io.BitcoinInput
import bitcoin.walllet.kit.common.io.BitcoinOutput
import bitcoin.walllet.kit.common.serializer.HashSerializer
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException

/**
 * An outpoint describes the transaction output that is connected to a transaction input.
 * It consists of the hash for the transaction containing the output
 * and the index of the output within the transaction.
 */
class OutPoint @Throws(IOException::class) constructor(input: BitcoinInput) {

    // 32-bytes, the hash of the referenced transaction.
    @JsonSerialize(using = HashSerializer::class)
    var hash: ByteArray = input.readBytes(32)

    // Uint32, the index of the specific output in the transaction. The first output is 0, etc.
    var index: Long = input.readUnsignedInt()

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(hash)
                .writeUnsignedInt(index)
                .toByteArray()
    }
}
