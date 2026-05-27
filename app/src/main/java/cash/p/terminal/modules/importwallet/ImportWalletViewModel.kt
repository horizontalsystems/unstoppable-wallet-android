package cash.p.terminal.modules.importwallet

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.managers.SeedPhraseQrCrypto
import cash.p.terminal.core.managers.toSeedQrErrorStringRes
import cash.p.terminal.core.openInputStreamSafe
import cash.p.terminal.core.utils.Bip39LanguageDetector
import cash.p.terminal.core.validateAndSaveBackup
import cash.p.terminal.strings.helpers.Translator
import cash.p.terminal.wallet.normalizeNFKD
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ImportWalletViewModel(
    private val seedPhraseQrCrypto: SeedPhraseQrCrypto,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _navigationEvents = Channel<NavigationEvent>(Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var backupFileError by mutableStateOf(false)
        private set

    fun processBackupFile(contentResolver: ContentResolver, uri: Uri, fileName: String?) {
        viewModelScope.launch(dispatcherProvider.io) {
            try {
                contentResolver.openInputStreamSafe(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val backupFilePath = validateAndSaveBackup(bytes)
                    _navigationEvents.send(
                        NavigationEvent.OpenRestoreLocal(backupFilePath, fileName)
                    )
                } ?: throw IllegalStateException("Cannot open input stream")
            } catch (e: Throwable) {
                backupFileError = true
            }
        }
    }

    fun onBackupFileErrorShown() {
        backupFileError = false
    }

    fun handleScannedData(scannedText: String) {
        if (scannedText.startsWith(SeedPhraseQrCrypto.QR_PREFIX)) {
            seedPhraseQrCrypto.decrypt(scannedText).onSuccess(::openRestoreFromQr)
                .onFailure { error ->
                    errorMessage = Translator.getString(error.toSeedQrErrorStringRes())
                }
        } else {
            val mnemonic = scannedText.toPlainBip39Mnemonic()
            if (mnemonic != null) {
                openRestoreFromQr(mnemonic)
            } else {
                errorMessage = Translator.getString(R.string.seed_qr_invalid_format)
            }
        }
    }

    fun onErrorShown() {
        errorMessage = null
    }

    private fun openRestoreFromQr(seed: SeedPhraseQrCrypto.DecryptedSeed) {
        viewModelScope.launch {
            _navigationEvents.send(
                NavigationEvent.OpenRestoreFromQr(
                    words = seed.words,
                    passphrase = seed.passphrase,
                    moneroHeight = seed.height,
                    language = seed.language
                )
            )
        }
    }

    private fun String.toPlainBip39Mnemonic(): SeedPhraseQrCrypto.DecryptedSeed? {
        val words = trim().lowercase().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.normalizeNFKD() }
        if (words.size !in BIP39_WORD_COUNTS) return null

        val language = Bip39LanguageDetector.detectExact(words).firstOrNull() ?: return null
        return SeedPhraseQrCrypto.DecryptedSeed(
            words = words,
            passphrase = "",
            height = null,
            language = language
        )
    }

    sealed class NavigationEvent {
        data class OpenRestoreFromQr(
            val words: List<String>,
            val passphrase: String,
            val moneroHeight: Long?,
            val language: Language?
        ) : NavigationEvent()

        data class OpenRestoreLocal(
            val backupFilePath: String,
            val fileName: String?
        ) : NavigationEvent()
    }

    private companion object {
        val BIP39_WORD_COUNTS = Mnemonic.EntropyStrength.entries.map { it.wordCount }.toSet()
    }
}
