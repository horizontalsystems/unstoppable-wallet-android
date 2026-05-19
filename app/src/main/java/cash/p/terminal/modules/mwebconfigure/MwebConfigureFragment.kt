package cash.p.terminal.modules.mwebconfigure

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.addCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.title
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.modules.moneroconfigure.MoneroConfigureScreen
import cash.p.terminal.navigation.setNavigationResultX
import cash.p.terminal.navigation.popBackStackSafely
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.parcelize.Parcelize
import org.koin.compose.viewmodel.koinViewModel

class MwebConfigureFragment : BaseComposeFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        activity?.onBackPressedDispatcher?.addCallback(this) {
            isEnabled = false
            close(findNavController())
        }
    }

    @Composable
    override fun GetContent(navController: NavController) {
        val initialConfig = navController.getInput<Input>()?.initialConfig
        val viewModel: MwebConfigureViewModel = koinViewModel()

        LaunchedEffect(initialConfig) {
            viewModel.setInitialConfig(initialConfig)
        }

        MoneroConfigureScreen(
            title = TokenType.Mweb.title,
            blockchainType = BlockchainType.Litecoin,
            heightHintRes = R.string.restoreheight_hint_block_only,
            onCloseWithResult = {
                viewModel.onClosed()
                closeWithConfig(it, navController)
            },
            onCloseClick = { close(navController) },
            onRestoreNew = viewModel::onRestoreNew,
            onSetBirthdayHeight = viewModel::setBirthdayHeight,
            onDoneClick = viewModel::onDoneClick,
            uiState = viewModel.uiState,
        )
    }

    private fun closeWithConfig(config: TokenConfig, navController: NavController) {
        navController.setNavigationResultX(Result(config))
        navController.popBackStack()
    }

    private fun close(navController: NavController) {
        navController.setNavigationResultX(Result(null))
        navController.popBackStackSafely()
    }

    @Parcelize
    data class Result(val config: TokenConfig?) : Parcelable

    @Parcelize
    data class Input(val initialConfig: TokenConfig?) : Parcelable
}
