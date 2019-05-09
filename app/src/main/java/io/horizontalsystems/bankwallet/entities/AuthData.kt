package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import io.horizontalsystems.hdwalletkit.Mnemonic
import java.util.*

class AuthData {
    var words: List<String> = listOf()
    var walletId: String = ""
    var seed: ByteArray = byteArrayOf()
    private var version = 2

    private val wordsSeparator = " "
    private val partsSeparator = "|"

    constructor(words: List<String>, walletId: String = UUID.randomUUID().toString()) {
        this.words = words
        this.walletId = walletId
        this.seed = Mnemonic().toSeed(words)
    }

    constructor(serialized: String) {
        if (!serialized.contains(partsSeparator)) {
            val wordsAndWalletId = serialized.split(wordsSeparator)
            version = 1
            words = wordsAndWalletId.subList(0, 12)
            wordsAndWalletId.getOrNull(12)?.let { walletId = it }
            seed = Mnemonic().toSeed(words)
        } else {
            val (version, wordsString, walletId, seedString) = serialized.split(partsSeparator)

            this.version = version.toInt()
            this.words = wordsString.split(wordsSeparator)
            this.walletId = walletId
            this.seed = seedString.hexToByteArray()
        }
    }

    override fun toString(): String {
        return listOf(version, words.joinToString(wordsSeparator), walletId, seed.toHexString()).joinToString(partsSeparator)
    }
}
