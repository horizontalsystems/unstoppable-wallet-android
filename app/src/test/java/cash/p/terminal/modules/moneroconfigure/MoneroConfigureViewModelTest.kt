package cash.p.terminal.modules.moneroconfigure

import cash.p.terminal.core.usecase.ValidateMoneroHeightUseCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MoneroConfigureViewModelTest {

    private val validateMoneroHeightUseCase = mockk<ValidateMoneroHeightUseCase>()

    @Test
    fun onClosed_closeResultSet_clearsCloseResult() {
        every { validateMoneroHeightUseCase.getTodayHeight() } returns 3_000_000L
        val viewModel = MoneroConfigureViewModel(validateMoneroHeightUseCase)

        viewModel.onDoneClick()
        assertNotNull(viewModel.uiState.closeWithResult)

        viewModel.onClosed()

        assertNull(viewModel.uiState.closeWithResult)
    }

    @Test
    fun onDoneClick_restoreAsNew_usesTodayHeight() {
        every { validateMoneroHeightUseCase.getTodayHeight() } returns 3_000_000L
        val viewModel = MoneroConfigureViewModel(validateMoneroHeightUseCase)

        viewModel.setBirthdayHeight("1")
        viewModel.onDoneClick()

        assertEquals("3000000", viewModel.uiState.closeWithResult?.birthdayHeight)
    }
}
