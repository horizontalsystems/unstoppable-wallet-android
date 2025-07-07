package cash.p.terminal.core.usecase

import cash.p.terminal.core.managers.WordsManager
import java.util.zip.CRC32

class ValidateMoneroMnemonicUseCase(
    private val commonWordsManager: WordsManager
) {
    operator fun invoke(mnemonicWords: List<String>, isMonero: Boolean) {
        if (isMonero) {
            validateMoneroChecksum(mnemonicWords)
        } else {
            commonWordsManager.validateChecksumStrict(mnemonicWords)
        }
    }

    private fun validateMoneroChecksum(mnemonicWords: List<String>) {
        if (mnemonicWords.size != 25) {
            throw IllegalArgumentException("Monero mnemonic must be 25 words long")
        }

        val prefixLength = 3
        val words24 = mnemonicWords.take(24)
        val checksumWord = mnemonicWords[24]

        val concatenated = words24.joinToString("") { it.take(prefixLength) }

        val crc = CRC32()
        crc.update(concatenated.toByteArray(Charsets.UTF_8))
        val checksumIndex = (crc.value % 24).toInt()

        val expectedChecksumWord = words24[checksumIndex]

        if (expectedChecksumWord != checksumWord) {
            throw IllegalArgumentException(
                "Invalid Monero checksum: expected \"$expectedChecksumWord\" at position 25, got \"$checksumWord\""
            )
        }
    }
}