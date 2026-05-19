package cash.p.terminal.modules.enablecoin.restoresettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.moneroconfigure.MoneroConfigureFragment
import cash.p.terminal.modules.mwebconfigure.MwebConfigureFragment
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureFragment
import cash.p.terminal.navigation.slideFromBottomForResult
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.isLitecoinMweb
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.delay

@Composable
fun NavController.openRestoreSettingsDialog(
    token: Token?,
    restoreSettingsViewModel: IRestoreSettingsUi
) {
    val keyboard = LocalSoftwareKeyboardController.current

    fun handleResult(config: TokenConfig?) {
        if (config != null) {
            restoreSettingsViewModel.onEnter(config)
        } else {
            restoreSettingsViewModel.onCancelEnterBirthdayHeight()
        }
    }

    LaunchedEffect(token) {
        val tokenToConfigure = token ?: return@LaunchedEffect
        keyboard?.hide()
        // Let IME finish closing before opening the bottom sheet.
        delay(300)

        restoreSettingsViewModel.tokenConfigureOpened()
        val initialConfig = restoreSettingsViewModel.consumeInitialConfig()

        when (tokenToConfigure.blockchainType) {
            BlockchainType.Zcash -> {
                slideFromBottomForResult<ZcashConfigureFragment.Result>(
                    resId = R.id.zcashConfigureFragment,
                    input = ZcashConfigureFragment.Input(initialConfig)
                ) { result ->
                    handleResult(result.config)
                }
            }

            BlockchainType.Monero -> {
                slideFromBottomForResult<MoneroConfigureFragment.Result>(
                    resId = R.id.moneroConfigure,
                    input = MoneroConfigureFragment.Input(initialConfig)
                ) { result ->
                    handleResult(result.config)
                }
            }

            BlockchainType.Litecoin -> {
                if (tokenToConfigure.isLitecoinMweb) {
                    slideFromBottomForResult<MwebConfigureFragment.Result>(
                        resId = R.id.mwebConfigure,
                        input = MwebConfigureFragment.Input(initialConfig)
                    ) { result ->
                        handleResult(result.config)
                    }
                }
            }

            else -> Unit
        }
    }
}
