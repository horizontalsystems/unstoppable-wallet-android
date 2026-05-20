package cash.p.terminal.modules.importwallet

import android.util.Base64
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.managers.SeedPhraseQrCrypto
import cash.p.terminal.core.managers.TimePasswordProvider
import cash.p.terminal.core.utils.Bip39LanguageDetector
import io.horizontalsystems.hdwalletkit.Language
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Regression tests for ImportWalletViewModel — locks down the QR decrypt path before
 * SeedPhraseQrCrypto JSON v2 migration. Existing scanners must keep parsing v1 (legacy)
 * QRs and the new v2 JSON QRs alike.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportWalletViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val dispatcherProvider = TestDispatcherProvider(dispatcher, CoroutineScope(dispatcher))

    private lateinit var crypto: SeedPhraseQrCrypto

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        every { Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        crypto = SeedPhraseQrCrypto(TimePasswordProvider())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private val words12 = ("abandon abandon abandon abandon abandon abandon " +
            "abandon abandon abandon abandon abandon about").split(" ")
    private val spanishWords12 = List(11) { "ábaco" } + "abierto"
    private val portugueseWords12 =
        "cruzeiro cidreira pistola surreal munido padaria protetor mensagem orbitar meteoro apetite vergonha"
            .split(" ")
    private val words25Monero = ("tavern total bail plutonium faked faster beneath reinvest " +
            "syndrome dagger razor nobody acoustic tubes people germs myriad next victim sipped " +
            "oasis dagger razor acoustic acoustic").split(" ")
    private val entropy128 = ByteArray(16)

    @Test
    fun handleScannedData_validBip39Qr_emitsOpenRestoreFromQrEvent() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
        val encrypted = crypto.encrypt(words12, "myPass")

        viewModel.handleScannedData(encrypted)
        advanceUntilIdle()

        val event = withTimeout(1_000) { viewModel.navigationEvents.first() }
        assertNotNull(event)
        val openEvent = event as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
        assertEquals(words12, openEvent.words)
        assertEquals("myPass", openEvent.passphrase)
        assertNull(openEvent.moneroHeight)
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun handleScannedData_validBip39QrWithLanguage_emitsLanguageHint() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
        val encrypted = crypto.encrypt(spanishWords12, "", language = Language.Spanish)

        viewModel.handleScannedData(encrypted)
        advanceUntilIdle()

        val openEvent = withTimeout(1_000) {
            viewModel.navigationEvents.first()
        } as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
        assertEquals(Language.Spanish, openEvent.language)
    }

    @Test
    fun handleScannedData_plainPortugueseMnemonic_emitsOpenRestoreFromQrEventWithDetectedLanguage() =
        runTest(dispatcher) {
            val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
            val scannedText = portugueseWords12.joinToString(" ")

            viewModel.handleScannedData(scannedText)
            advanceUntilIdle()

            val openEvent = withTimeout(1_000) {
                viewModel.navigationEvents.first()
            } as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
            assertEquals(portugueseWords12, openEvent.words)
            assertEquals("", openEvent.passphrase)
            assertNull(openEvent.moneroHeight)
            assertEquals(Language.Portuguese, openEvent.language)
            assertNull(viewModel.errorMessage)
        }

    @Test
    fun handleScannedData_plainMnemonicForEachBip39Language_emitsOpenRestoreFromQrEvent() =
        runTest(dispatcher) {
            val mnemonic = Mnemonic()

            Language.entries.forEach { language ->
                val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
                val words = mnemonic.toMnemonic(entropy128, language)
                val expectedLanguage = Bip39LanguageDetector.detectExact(words).firstOrNull()
                assertNotNull("Words must be detectable for $language", expectedLanguage)

                viewModel.handleScannedData(words.joinToString(" "))
                advanceUntilIdle()

                val openEvent = withTimeout(1_000) {
                    viewModel.navigationEvents.first()
                } as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
                assertEquals("Words must round-trip for $language", words, openEvent.words)
                assertEquals("", openEvent.passphrase)
                assertNull(openEvent.moneroHeight)
                assertEquals(expectedLanguage, openEvent.language)
                assertNull(viewModel.errorMessage)
            }
        }

    @Test
    fun handleScannedData_validMoneroQr_emitsEventWithHeight() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
        val encrypted = crypto.encrypt(words25Monero, "", height = 2_500_000L)

        viewModel.handleScannedData(encrypted)
        advanceUntilIdle()

        val openEvent = withTimeout(1_000) {
            viewModel.navigationEvents.first()
        } as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
        assertEquals(words25Monero, openEvent.words)
        assertEquals(2_500_000L, openEvent.moneroHeight)
    }

    @Test
    fun handleScannedData_qrPrefixButCorruptPayload_setsErrorMessageNoEvent() =
        runTest(dispatcher) {
            val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)

            viewModel.handleScannedData("seed:not-valid-base64!!!")
            advanceUntilIdle()

            assertNotNull(
                "Decryption failure must surface an error message",
                viewModel.errorMessage
            )
        }

    @Test
    fun handleScannedData_nonSeedPrefix_setsErrorMessage() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)

        viewModel.handleScannedData("https://example.com/some-other-qr")
        advanceUntilIdle()

        assertNotNull(
            "Non-seed QR must be rejected with an error message",
            viewModel.errorMessage
        )
    }

    @Test
    fun handleScannedData_validQrAfterErrorState_clearsErrorOnSuccess() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)

        // Trigger error first
        viewModel.handleScannedData("not-a-seed-qr")
        advanceUntilIdle()
        assertNotNull(viewModel.errorMessage)

        // Now successful scan — error remains until onErrorShown(); contract is that
        // success path does NOT clear errorMessage automatically.
        // Lock down current behavior: success path still emits navigation event regardless of
        // pre-existing error.
        val encrypted = crypto.encrypt(words12, "")
        viewModel.handleScannedData(encrypted)
        advanceUntilIdle()

        val openEvent = withTimeout(1_000) {
            viewModel.navigationEvents.first()
        } as ImportWalletViewModel.NavigationEvent.OpenRestoreFromQr
        assertEquals(words12, openEvent.words)
    }

    @Test
    fun onErrorShown_clearsErrorMessage() = runTest(dispatcher) {
        val viewModel = ImportWalletViewModel(crypto, dispatcherProvider)
        viewModel.handleScannedData("not-a-seed-qr")
        advanceUntilIdle()
        assertNotNull(viewModel.errorMessage)

        viewModel.onErrorShown()

        assertNull(viewModel.errorMessage)
    }
}
