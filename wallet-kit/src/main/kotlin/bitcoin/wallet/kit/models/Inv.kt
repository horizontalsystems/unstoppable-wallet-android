package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.common.io.BitcoinInput
import java.io.IOException

class Inv {

    var inventory: Array<InvVect>

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        val count = input.readVarInt() // do not store count
        inventory = Array(count.toInt()) {
            InvVect(input)
        }
    }
}
