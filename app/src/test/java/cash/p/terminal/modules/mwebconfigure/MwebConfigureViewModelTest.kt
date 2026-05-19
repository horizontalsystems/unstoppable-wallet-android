package cash.p.terminal.modules.mwebconfigure

import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MwebConfigureViewModelTest {

    @Test
    fun onDoneClick_restoreAsNew_returnsConfigWithoutBirthdayHeight() {
        val viewModel = MwebConfigureViewModel()

        viewModel.setBirthdayHeight("2257920")
        viewModel.onDoneClick()

        val result = viewModel.uiState.closeWithResult
        assertNull(result?.birthdayHeight)
        assertEquals(true, result?.restoreAsNew)
        assertNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun onDoneClick_existingWalletValidHeight_returnsBirthdayHeight() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onRestoreNew(false)
        viewModel.setBirthdayHeight("2257920")
        viewModel.onDoneClick()

        val result = viewModel.uiState.closeWithResult
        assertEquals("2257920", result?.birthdayHeight)
        assertFalse(result?.restoreAsNew ?: true)
        assertNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun onDoneClick_existingWalletLargeHeight_returnsBirthdayHeight() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onRestoreNew(false)
        viewModel.setBirthdayHeight("100000000")
        viewModel.onDoneClick()

        val result = viewModel.uiState.closeWithResult
        assertEquals("100000000", result?.birthdayHeight)
        assertFalse(result?.restoreAsNew ?: true)
        assertNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun onDoneClick_existingWalletHeightAboveIntRange_setsError() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onRestoreNew(false)
        viewModel.setBirthdayHeight((Int.MAX_VALUE.toLong() + 1).toString())
        viewModel.onDoneClick()

        assertNull(viewModel.uiState.closeWithResult)
        assertNotNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun onDoneClick_existingWalletEmptyHeight_setsError() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onRestoreNew(false)
        viewModel.setBirthdayHeight("")
        viewModel.onDoneClick()

        assertNull(viewModel.uiState.closeWithResult)
        assertNotNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun onDoneClick_existingWalletNonNumericHeight_setsError() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onRestoreNew(false)
        viewModel.setBirthdayHeight("not-a-height")
        viewModel.onDoneClick()

        assertNull(viewModel.uiState.closeWithResult)
        assertNotNull(viewModel.uiState.errorHeight)
    }

    @Test
    fun setInitialConfig_existingHeight_updatesState() {
        val viewModel = MwebConfigureViewModel()

        viewModel.setInitialConfig(
            TokenConfig(
                birthdayHeight = "2300000",
                restoreAsNew = false
            )
        )

        assertEquals("2300000", viewModel.uiState.birthdayHeight)
        assertFalse(viewModel.uiState.restoreAsNew)
        assertNull(viewModel.uiState.errorHeight)
        assertNull(viewModel.uiState.closeWithResult)
    }

    @Test
    fun onClosed_closeResultSet_clearsCloseResult() {
        val viewModel = MwebConfigureViewModel()

        viewModel.onDoneClick()
        assertNotNull(viewModel.uiState.closeWithResult)

        viewModel.onClosed()

        assertNull(viewModel.uiState.closeWithResult)
    }
}
