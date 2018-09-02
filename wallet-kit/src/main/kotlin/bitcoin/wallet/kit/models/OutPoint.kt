package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import bitcoin.walllet.kit.io.BitcoinOutput
import io.realm.RealmObject
import java.io.IOException

/**
 * An outpoint describes the transaction output that is connected to a transaction input.
 * It consists of the hash for the transaction containing the output
 * and the index of the output within the transaction.
 */
open class OutPoint : RealmObject {

    // 32-bytes, the hash of the referenced transaction.
    // @JsonSerialize(using = HashSerializer::class)
    var hash: ByteArray = byteArrayOf()

    // Uint32, the index of the specific output in the transaction. The first output is 0, etc.
    var index: Long = 0

    constructor()

    @Throws(IOException::class) constructor(input: BitcoinInput) {
        this.hash = input.readBytes(32)
        this.index = input.readUnsignedInt()
    }

    fun toByteArray(): ByteArray {
        return BitcoinOutput()
                .write(hash)
                .writeUnsignedInt(index)
                .toByteArray()
    }
}
