package cash.p.terminal.core.managers

import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import com.m2049r.xmrwallet.service.MoneroWalletService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MoneroKitWrapperRefreshTest {

    private val restoreSettingsManager = mockk<RestoreSettingsManager>(relaxed = true)
    private val account = Account(
        id = "account-id",
        name = "Monero",
        type = AccountType.MnemonicMonero(
            words = emptyList(),
            password = "password",
            height = 1,
            walletInnerName = "wallet"
        ),
        origin = AccountOrigin.Created,
        level = 0
    )
    private val unsupportedAccount = account.copy(
        type = AccountType.EvmAddress("0x1234")
    )

    @Test
    fun refresh_syncingWallet_doesNotStopService() = runTest {
        val service = mockService()
        val wrapper = createWrapper(service)

        wrapper.refresh()

        verify(exactly = 0) { service.stop(any()) }
    }

    @Test
    fun refresh_pausedWalletFailedResume_restartsWallet() = runTest {
        val service = mockService()
        val wrapper = createWrapper(service, unsupportedAccount)
        every { service.resume(wrapper) } returns false

        setStarted(wrapper)
        setSyncState(wrapper, AdapterState.Synced)
        wrapper.pause()
        wrapper.refresh()

        verify(exactly = 1) { service.resume(wrapper) }
        verify(exactly = 1) { service.stop(false) }
    }

    private fun mockService(): MoneroWalletService {
        return mockk(relaxed = true)
    }

    private fun createWrapper(
        service: MoneroWalletService,
        account: Account = this.account
    ): MoneroKitWrapper {
        return MoneroKitWrapper(
            moneroWalletService = service,
            restoreSettingsManager = restoreSettingsManager,
            account = account
        )
    }

    private fun setStarted(wrapper: MoneroKitWrapper) {
        privateField(wrapper, "isStarted").set(wrapper, true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun setSyncState(wrapper: MoneroKitWrapper, state: AdapterState) {
        val syncState = privateField(wrapper, "_syncState")
            .get(wrapper) as MutableStateFlow<AdapterState>
        syncState.value = state
    }

    private fun privateField(wrapper: MoneroKitWrapper, name: String) =
        wrapper.javaClass.getDeclaredField(name).apply {
            isAccessible = true
        }
}
