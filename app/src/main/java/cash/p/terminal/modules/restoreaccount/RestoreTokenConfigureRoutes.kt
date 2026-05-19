package cash.p.terminal.modules.restoreaccount

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import cash.p.terminal.R
import cash.p.terminal.core.composablePopup
import cash.p.terminal.core.title
import cash.p.terminal.modules.enablecoin.restoresettings.TokenConfig
import cash.p.terminal.modules.moneroconfigure.MoneroConfigureScreen
import cash.p.terminal.modules.moneroconfigure.MoneroConfigureViewModel
import cash.p.terminal.modules.mwebconfigure.MwebConfigureViewModel
import cash.p.terminal.modules.zcashconfigure.ZcashConfigureScreen
import cash.p.terminal.navigation.popBackStackSafely
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.isLitecoinMweb
import io.horizontalsystems.core.entities.BlockchainType
import org.koin.compose.viewmodel.koinViewModel

private const val ROUTE_ZCASH_CONFIGURE = "zcash_configure"
private const val ROUTE_MONERO_CONFIGURE = "monero_configure"
private const val ROUTE_MWEB_CONFIGURE = "mweb_configure"

internal fun NavController.openRestoreTokenConfigure(
    token: Token,
    initialConfig: TokenConfig?,
    mainViewModel: RestoreViewModel,
) {
    mainViewModel.setTokenInitialConfig(initialConfig)
    when (token.blockchainType) {
        BlockchainType.Zcash -> navigate(ROUTE_ZCASH_CONFIGURE)
        BlockchainType.Monero -> navigate(ROUTE_MONERO_CONFIGURE)
        BlockchainType.Litecoin -> {
            if (token.isLitecoinMweb) {
                navigate(ROUTE_MWEB_CONFIGURE)
            }
        }

        else -> Unit
    }
}

internal fun NavGraphBuilder.addRestoreTokenConfigureRoutes(
    navController: NavController,
    mainViewModel: RestoreViewModel,
) {
    composablePopup(ROUTE_ZCASH_CONFIGURE) {
        ZcashConfigureScreen(
            initialConfig = mainViewModel.tokenInitialConfig,
            onCloseWithResult = { config ->
                mainViewModel.setTokenConfig(config)
                navController.popBackStackSafely()
            },
            onCloseClick = {
                mainViewModel.cancelTokenConfig()
                navController.popBackStackSafely()
            }
        )
    }
    composablePopup(ROUTE_MONERO_CONFIGURE) {
        val viewModel: MoneroConfigureViewModel = koinViewModel()
        LaunchedEffect(mainViewModel.tokenInitialConfig) {
            viewModel.setInitialConfig(mainViewModel.tokenInitialConfig)
        }
        MoneroConfigureScreen(
            onCloseWithResult = {
                mainViewModel.setTokenConfig(it)
                navController.popBackStackSafely()
            },
            onCloseClick = {
                mainViewModel.cancelTokenConfig()
                navController.popBackStackSafely()
            },
            onRestoreNew = viewModel::onRestoreNew,
            onSetBirthdayHeight = viewModel::setBirthdayHeight,
            onDoneClick = viewModel::onDoneClick,
            uiState = viewModel.uiState,
        )
    }
    composablePopup(ROUTE_MWEB_CONFIGURE) {
        val viewModel: MwebConfigureViewModel = koinViewModel()
        LaunchedEffect(mainViewModel.tokenInitialConfig) {
            viewModel.setInitialConfig(mainViewModel.tokenInitialConfig)
        }
        MoneroConfigureScreen(
            title = TokenType.Mweb.title,
            blockchainType = BlockchainType.Litecoin,
            heightHintRes = R.string.restoreheight_hint_block_only,
            onCloseWithResult = {
                mainViewModel.setTokenConfig(it)
                navController.popBackStackSafely()
            },
            onCloseClick = {
                mainViewModel.cancelTokenConfig()
                navController.popBackStackSafely()
            },
            onRestoreNew = viewModel::onRestoreNew,
            onSetBirthdayHeight = viewModel::setBirthdayHeight,
            onDoneClick = viewModel::onDoneClick,
            uiState = viewModel.uiState,
        )
    }
}
