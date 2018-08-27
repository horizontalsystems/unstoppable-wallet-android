package bitcoin.wallet.kit.models

import bitcoin.walllet.kit.io.BitcoinInput
import java.io.IOException

class Inv {

    var inventory: Array<InventoryItem>

    @Throws(IOException::class)
    constructor(input: BitcoinInput) {
        val count = input.readVarInt() // do not store count
        inventory = Array(count.toInt()) {
            InventoryItem(input)
        }
    }
}
