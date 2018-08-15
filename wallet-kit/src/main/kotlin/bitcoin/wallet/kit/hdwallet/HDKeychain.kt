package bitcoin.wallet.kit.hdwallet

import bitcoin.walllet.kit.common.hdwallet.HDKey
import bitcoin.walllet.kit.common.hdwallet.HDKeyDerivation

class HDKeychain(seed: ByteArray) {

    private var privateKey: HDKey = HDKeyDerivation.createRootKey(seed)

    /// Parses the BIP32 path and derives the chain of keychains accordingly.
    /// Path syntax: (m?/)?([0-9]+'?(/[0-9]+'?)*)?
    /// The following paths are valid:
    ///
    /// "" (root key)
    /// "m" (root key)
    /// "/" (root key)
    /// "m/0'" (hardened child #0 of the root key)
    /// "/0'" (hardened child #0 of the root key)
    /// "0'" (hardened child #0 of the root key)
    /// "m/44'/1'/2'" (BIP44 testnet account #2)
    /// "/44'/1'/2'" (BIP44 testnet account #2)
    /// "44'/1'/2'" (BIP44 testnet account #2)
    ///
    /// The following paths are invalid:
    ///
    /// "m / 0 / 1" (contains spaces)
    /// "m/b/c" (alphabetical characters instead of numerical indexes)
    /// "m/1.2^3" (contains illegal characters)
    fun getKeyByPath(derivePath: String): HDKey {
        var key = privateKey

        var path = derivePath
        if (path == "m" || path == "/" || path == "") {
            return key
        }
        if (path.contains("m/")) {
            path = path.drop(2)
        }
        for (chunk in path.split("/")) {
            var hardened = false
            var indexText: String = chunk
            if (chunk.contains("'")) {
                hardened = true
                indexText = indexText.dropLast(1)
            }
            val index = indexText.toInt()
            key = HDKeyDerivation.deriveChildKey(key, index, hardened)
        }

//        print("\nkey.serializePrivKeyToString: " + key.serializePrivKeyToString())
        return key
    }

}
