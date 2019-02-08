package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bitcoinkit.core.hexStringToByteArray
import io.horizontalsystems.bitcoinkit.core.toHexString
import io.horizontalsystems.hdwalletkit.Mnemonic
import java.util.*

class AuthData {
    var words: List<String> = listOf()
    var walletId: String = ""
    var seed: ByteArray = byteArrayOf()

    private val wordsSeparator = ","
    private val partsSeparator = ";"

    constructor(words: List<String>, walletId: String = UUID.randomUUID().toString()) {
        this.words = words
        this.walletId = walletId
        this.seed = Mnemonic().toSeed(words)
    }

    constructor(serialized: String) {
        val (wordsString, walletId, seedString) = serialized.split(partsSeparator)

        this.words = wordsString.split(wordsSeparator)
        this.walletId = walletId
        this.seed = seedString.hexStringToByteArray()
    }

    override fun toString(): String {
        return listOf(words.joinToString(wordsSeparator), walletId, seed.toHexString()).joinToString(partsSeparator)
    }
}
