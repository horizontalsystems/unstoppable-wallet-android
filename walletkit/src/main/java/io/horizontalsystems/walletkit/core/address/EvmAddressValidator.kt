package io.horizontalsystems.walletkit.core.address

import org.bouncycastle.crypto.digests.KeccakDigest

/**
 * Kit-free EIP-55 address validation. Mirrors ethereum-kit's AddressValidator:
 * requires the 0x-prefixed 40-hex form and, when the hex part is mixed-case,
 * verifies the EIP-55 checksum.
 */
object EvmAddressValidator {

    private val addressRegex = Regex("^0x[0-9a-fA-F]{40}$")

    @Throws(AddressValidationException::class)
    fun validate(address: String) {
        if (!addressRegex.matches(address)) {
            throw AddressValidationException("Invalid address format: $address")
        }

        val hex = address.substring(2)
        if (hex == hex.lowercase() || hex == hex.uppercase()) return

        val hash = keccak256(hex.lowercase().toByteArray(Charsets.US_ASCII)).toHex()
        hex.forEachIndexed { index, char ->
            val hashChar = hash[index]
            val checksummed = if (Character.digit(hashChar, 16) >= 8) char.uppercaseChar() else char.lowercaseChar()
            if (char != checksummed) {
                throw AddressValidationException("Invalid checksum: $address")
            }
        }
    }

    private fun keccak256(input: ByteArray): ByteArray {
        val digest = KeccakDigest(256)
        digest.update(input, 0, input.size)
        val output = ByteArray(digest.digestSize)
        digest.doFinal(output, 0)
        return output
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    class AddressValidationException(message: String) : Exception(message)
}
