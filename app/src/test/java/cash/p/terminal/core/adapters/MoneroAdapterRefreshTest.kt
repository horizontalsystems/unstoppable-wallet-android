package cash.p.terminal.core.adapters

import cash.p.terminal.core.managers.MoneroKitWrapper
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MoneroAdapterRefreshTest {

    @Test
    fun refresh_requested_delegatesToMoneroKitWrapper() = runTest {
        val moneroKitWrapper = mockk<MoneroKitWrapper>(relaxed = true)
        val adapter = MoneroAdapter(moneroKitWrapper)

        adapter.refresh()

        coVerify(exactly = 1) { moneroKitWrapper.refresh() }
    }
}
