package cash.p.terminal.modules.restoreaccount

import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertIs

class RestoreViewModelTest {

    @Test
    fun tokenConfigResult_afterSet_publishesEnteredResultUntilHandled() {
        val viewModel = RestoreViewModel()
        val initialConfig = TokenConfig("100", restoreAsNew = false)
        val resultConfig = TokenConfig("200", restoreAsNew = false)

        viewModel.setTokenInitialConfig(initialConfig)
        assertEquals("100", viewModel.tokenInitialConfig?.birthdayHeight)

        viewModel.setTokenConfig(resultConfig)

        assertNull(viewModel.tokenInitialConfig)
        val result = assertIs<TokenConfigResult.Entered>(viewModel.tokenConfigResult.value)
        assertEquals(resultConfig, result.config)

        viewModel.clearTokenConfigResult(result.id)

        assertNull(viewModel.tokenConfigResult.value)
    }

    @Test
    fun tokenConfigResult_afterCancel_publishesCancelledResultUntilHandled() {
        val viewModel = RestoreViewModel()

        viewModel.setTokenInitialConfig(TokenConfig("100", restoreAsNew = false))
        assertEquals("100", viewModel.tokenInitialConfig?.birthdayHeight)

        viewModel.cancelTokenConfig()

        assertNull(viewModel.tokenInitialConfig)
        val result = assertIs<TokenConfigResult.Cancelled>(viewModel.tokenConfigResult.value)

        viewModel.clearTokenConfigResult(result.id)

        assertNull(viewModel.tokenConfigResult.value)
    }

    @Test
    fun tokenConfigResult_sameConfigEnteredTwice_publishesDistinctResults() {
        val viewModel = RestoreViewModel()
        val resultConfig = TokenConfig("200", restoreAsNew = false)

        viewModel.setTokenConfig(resultConfig)
        val first = assertIs<TokenConfigResult.Entered>(viewModel.tokenConfigResult.value)
        viewModel.clearTokenConfigResult(first.id)

        viewModel.setTokenConfig(resultConfig)
        val second = assertIs<TokenConfigResult.Entered>(viewModel.tokenConfigResult.value)

        assertEquals(resultConfig, second.config)
        assertEquals(first.id + 1, second.id)
    }
}
