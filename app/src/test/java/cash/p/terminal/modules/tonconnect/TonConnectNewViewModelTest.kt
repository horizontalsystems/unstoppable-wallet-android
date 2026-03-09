package cash.p.terminal.modules.tonconnect

import cash.p.terminal.core.TestDispatcherProvider
import io.horizontalsystems.core.DispatcherProvider
import cash.p.terminal.core.managers.TonConnectManager
import cash.p.terminal.core.managers.toTonWalletFullAccess
import cash.p.terminal.core.storage.HardwarePublicKeyStorage
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import com.tonapps.wallet.data.tonconnect.entities.DAppManifestEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppPayloadEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.tonkit.tonconnect.TonConnectKit
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TonConnectNewViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)
    private val testDispatcherProvider = TestDispatcherProvider(dispatcher, testScope)
    private lateinit var request: DAppRequestEntity

    private val tonConnectKit = mockk<TonConnectKit>(relaxed = true)
    private val tonConnectManager = mockk<TonConnectManager>(relaxed = true)
    private val hardwarePublicKeyStorage = mockk<HardwarePublicKeyStorage>(relaxed = true)
    private val accountManager = mockk<IAccountManager>(relaxed = true)

    @Before
    fun setUp() {
        clearMocks(
            tonConnectKit,
            tonConnectManager,
            hardwarePublicKeyStorage,
            accountManager,
        )

        stopKoin()
        startKoin {
            modules(
                module {
                    single { hardwarePublicKeyStorage }
                    single<IAccountManager> { accountManager }
                    single<DispatcherProvider> { testDispatcherProvider }
                }
            )
        }

        every { tonConnectManager.kit } returns tonConnectKit
        request = requestStub("https://ton.dapp/manifest.json")

        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads manifest and selects active ton account`() = runTest(dispatcher) {
        val tonAccount = tonAccount("ton-1")
        val hardwareAccount = hardwareTonAccount("hw-1")
        val watchAccount = watchAccount("watch-1")
        setAccounts(listOf(tonAccount, hardwareAccount, watchAccount), tonAccount)

        val manifest = manifest()
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(manifest, state.manifest)
        assertEquals(listOf(tonAccount, hardwareAccount), state.accounts)
        assertEquals(tonAccount, state.account)
        assertNull(state.error)
        assertTrue(state.connectEnabled)
    }

    @Test
    fun `init filters unsupported accounts`() = runTest(dispatcher) {
        val tonAccount = tonAccount("ton-filter")
        val hardwareAccount = hardwareTonAccount("hw-filter")
        val unsupported = watchAccount("watch-filter")
        setAccounts(listOf(tonAccount, unsupported, hardwareAccount), tonAccount)
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest()

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(listOf(tonAccount, hardwareAccount), state.accounts)
        assertFalse(state.accounts.contains(unsupported))
    }

    @Test
    fun `init falls back to first ton account when active unsupported`() = runTest(dispatcher) {
        val fallback = tonAccount("ton-fallback")
        val secondary = hardwareTonAccount("hw-fallback")
        val activeUnsupported = watchAccount("watch-active")
        setAccounts(listOf(activeUnsupported, fallback, secondary), activeUnsupported)
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest()

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertEquals(listOf(fallback, secondary), state.accounts)
        assertEquals(fallback, state.account)
    }

    @Test
    fun `init without ton accounts exposes error`() = runTest(dispatcher) {
        val watchAccount = watchAccount("watch-only")
        setAccounts(listOf(watchAccount), watchAccount)
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest()

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        val state = viewModel.uiState
        assertIs<NoTonAccountError>(state.error)
        assertTrue(state.accounts.isEmpty())
        assertFalse(state.connectEnabled)
    }

    @Test
    fun `connect forwards request to kit and finishes`() = runTest(dispatcher) {
        val tonAccount = tonAccount("ton-connect")
        setAccounts(listOf(tonAccount), tonAccount)

        val manifest = manifest()
        mockkStatic("cash.p.terminal.core.managers.TonKitManagerKt")
        every {
            any<Account>().toTonWalletFullAccess(any(), any())
        } returns mockk(relaxed = true)
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest
        coEvery { tonConnectKit.connect(any(), any(), any(), any()) } returns mockk(relaxed = true)

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        viewModel.connect()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertTrue(state.finish)
        assertNull(state.toast)
        coVerify(exactly = 1) {
            tonConnectKit.connect(request, manifest, tonAccount.id, any())
        }
    }

    @Test
    fun `connect surfaces toast on failure`() = runTest(dispatcher) {
        val tonAccount = tonAccount("ton-fail")
        setAccounts(listOf(tonAccount), tonAccount)

        val manifest = manifest()
        mockkStatic("cash.p.terminal.core.managers.TonKitManagerKt")
        every {
            any<Account>().toTonWalletFullAccess(any(), any())
        } returns mockk(relaxed = true)
        coEvery { tonConnectKit.getManifest(request.payload.manifestUrl) } returns manifest
        coEvery {
            tonConnectKit.connect(
                any(),
                any(),
                any(),
                any()
            )
        } throws IllegalStateException("boom")

        val viewModel = TonConnectNewViewModel(request, tonConnectKit)
        advanceUntilIdle()

        viewModel.connect()
        advanceUntilIdle()

        val state = viewModel.uiState
        assertFalse(state.finish)
        assertEquals("boom", state.toast)

        viewModel.onToastShow()
        advanceUntilIdle()

        assertNull(viewModel.uiState.toast)
    }

    private fun setAccounts(accounts: List<Account>, active: Account?) {
        every { accountManager.accounts } returns accounts
        every { accountManager.activeAccount } returns active
    }

    private fun tonAccount(id: String) = Account(
        id = id,
        name = "Ton $id",
        type = AccountType.Mnemonic(mnemonicWords, ""),
        origin = AccountOrigin.Created,
        level = 1,
        isBackedUp = true
    )

    private fun hardwareTonAccount(id: String) = Account(
        id = id,
        name = "Hardware $id",
        type = AccountType.HardwareCard("card$id", 1, "hw$id", 1),
        origin = AccountOrigin.Restored,
        level = 1,
        isBackedUp = true
    )

    private fun watchAccount(id: String) = Account(
        id = id,
        name = "Watch $id",
        type = AccountType.TonAddress("UQC${id}Hash"),
        origin = AccountOrigin.Restored,
        level = 1,
        isBackedUp = true
    )

    private fun manifest(url: String = "https://ton.dapp") = DAppManifestEntity(
        url = url,
        name = "Test dApp",
        iconUrl = "$url/icon.png",
        termsOfUseUrl = null,
        privacyPolicyUrl = null
    )

    private fun requestStub(manifestUrl: String): DAppRequestEntity {
        val payload = DAppPayloadEntity(manifestUrl = manifestUrl, items = emptyList())
        return mockk(relaxed = true) {
            every { this@mockk.payload } returns payload
            every { id } returns "request-id"
        }
    }

    private val mnemonicWords = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )
}
