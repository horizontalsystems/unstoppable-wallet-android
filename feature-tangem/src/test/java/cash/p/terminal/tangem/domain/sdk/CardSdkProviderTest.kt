package cash.p.terminal.tangem.domain.sdk

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import com.tangem.TangemSdk
import com.tangem.common.authentication.AuthenticationManager
import com.tangem.sdk.nfc.NfcManager
import com.tangem.sdk.nfc.NfcReader
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.IPinComponent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class CardSdkProviderTest {

    private val backgroundManager: BackgroundManager = mockk(relaxed = true)
    private val sdkInitializer: SdkInitializer = mockk()
    private val isLockedFlow = MutableStateFlow(false)
    private val pinComponent: IPinComponent = mockk(relaxed = true) {
        every { isLockedFlow } returns this@CardSdkProviderTest.isLockedFlow
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun register_calledTwice_callsOnStopAndOnDestroyOnPreviousNfcManager() {
        val activity1 = mockActivity()
        val activity2 = mockActivity()
        val components1 = mockComponents()
        val components2 = mockComponents()

        every { sdkInitializer.create(activity1) } returns components1
        every { sdkInitializer.create(activity2) } returns components2

        val provider = CardSdkProvider(backgroundManager, pinComponent, sdkInitializer)

        provider.register(activity1)
        provider.register(activity2)

        verify { components1.nfcManager.onStop(activity1) }
        verify { components1.nfcManager.onDestroy(activity1) }
    }

    @Test
    fun isLockedFlow_emitsTrue_stopsActiveNfcSession() {
        val activity = mockActivity()
        val components = mockComponents()

        every { sdkInitializer.create(activity) } returns components

        val provider = CardSdkProvider(backgroundManager, pinComponent, sdkInitializer)
        provider.register(activity)

        val reader = components.nfcManager.reader
        isLockedFlow.value = true

        verify { reader.stopSession(cancelled = true) }
    }

    @Test
    fun isLockedFlow_emitsFalse_doesNotStopNfcSession() {
        val activity = mockActivity()
        val components = mockComponents()

        every { sdkInitializer.create(activity) } returns components

        val provider = CardSdkProvider(backgroundManager, pinComponent, sdkInitializer)
        provider.register(activity)

        val reader = components.nfcManager.reader
        isLockedFlow.value = false

        verify(exactly = 0) { reader.stopSession(any()) }
    }

    @Test
    fun observerOnDestroy_naturalDestroy_doesNotRemoveNfcManagerFromLifecycle() {
        val activity = mockActivity()
        val lifecycle = activity.lifecycle
        val components = mockComponents()
        val observerSlot = slot<LifecycleObserver>()

        every { lifecycle.addObserver(capture(observerSlot)) } returns Unit
        every { sdkInitializer.create(activity) } returns components

        val provider = CardSdkProvider(backgroundManager, pinComponent, sdkInitializer)
        provider.register(activity)

        val cardSdkObserver = observerSlot.captured as DefaultLifecycleObserver
        cardSdkObserver.onDestroy(activity)

        verify(exactly = 0) { lifecycle.removeObserver(components.nfcManager) }
    }

    private fun mockActivity(): FragmentActivity {
        val lifecycleMock: Lifecycle = mockk(relaxed = true)
        return mockk<FragmentActivity>(relaxed = true).apply {
            every { isDestroyed } returns false
            every { isFinishing } returns false
            every { isChangingConfigurations } returns false
            every { lifecycle } returns lifecycleMock
        }
    }

    private fun mockComponents(): SdkInitializer.Components {
        val nfcManager: NfcManager = mockk(relaxed = true)
        val authenticationManager: AuthenticationManager = mockk(relaxed = true)
        val sdk: TangemSdk = mockk(relaxed = true)
        val reader: NfcReader = mockk(relaxed = true)
        every { nfcManager.reader } returns reader
        return SdkInitializer.Components(
            nfcManager = nfcManager,
            authenticationManager = authenticationManager,
            sdk = sdk,
        )
    }
}
