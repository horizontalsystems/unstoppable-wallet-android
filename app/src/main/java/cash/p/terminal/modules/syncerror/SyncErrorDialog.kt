package cash.p.terminal.modules.syncerror

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.isEvm
import cash.p.terminal.ui_compose.getInput
import cash.p.terminal.navigation.slideFromBottom
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.isMonero
import cash.p.terminal.ui_compose.components.ButtonPrimaryDefault
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.BaseComposableBottomSheetFragment
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.annotatedStringResource
import cash.p.terminal.ui_compose.components.subhead2_grey
import kotlinx.parcelize.Parcelize

class SyncErrorDialog : BaseComposableBottomSheetFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                val navController = findNavController()
                navController.getInput<Input>()?.let { input ->
                    SyncErrorScreen(navController, input.wallet, input.errorMessage ?: "")
                }
            }
        }
    }

    @Parcelize
    data class Input(val wallet: Wallet, val errorMessage: String?) : Parcelable
}

@Composable
private fun SyncErrorScreen(navController: NavController, wallet: Wallet, error: String) {
    val viewModel = viewModel<SyncErrorViewModel>(factory = SyncErrorModule.Factory(wallet))

    val context = LocalContext.current
    val view = LocalView.current
    val clipboardManager = LocalClipboardManager.current

    val errorDescription = when {
        wallet.token.isMonero() || wallet.token.blockchainType.isEvm -> annotatedStringResource(R.string.source_blocked_by_provider_error)
        viewModel.sourceChangeable -> annotatedStringResource(R.string.balance_sync_error_changeable_source)
        else -> annotatedStringResource(R.string.balance_sync_error_fixed_source)
    }

    SyncErrorContent(
        coinCode = wallet.coin.code,
        errorDescription = errorDescription,
        showChangeSourceButton = viewModel.sourceChangeable,
        onClose = navController::popBackStack,
        onRetry = {
            viewModel.retry()
            navController.popBackStack()
        },
        onChangeSource = {
            navController.popBackStack()

            val blockchainWrapper = viewModel.blockchainWrapper
            when (blockchainWrapper?.type) {
                SyncErrorModule.BlockchainWrapper.Type.Bitcoin -> {
                    navController.slideFromBottom(
                        R.id.btcBlockchainSettingsFragment,
                        blockchainWrapper.blockchain
                    )
                }

                SyncErrorModule.BlockchainWrapper.Type.Evm -> {
                    navController.slideFromBottom(
                        R.id.evmNetworkFragment,
                        blockchainWrapper.blockchain
                    )
                }

                else -> {}
            }
        },
        onReport = {
            navController.popBackStack()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(viewModel.reportEmail))
                putExtra(Intent.EXTRA_TEXT, viewModel.buildReportBody(error))
            }

            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                clipboardManager.setText(AnnotatedString(viewModel.reportEmail))
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_EmailAddressCopied)
            }
        }
    )
}

@Composable
private fun SyncErrorContent(
    coinCode: String,
    errorDescription: AnnotatedString,
    showChangeSourceButton: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onChangeSource: () -> Unit,
    onReport: () -> Unit,
) {
    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_red_24),
            title = stringResource(R.string.BalanceSyncError_Title) + " - $coinCode",
            onCloseClick = onClose
        ) {
            subhead2_grey(
                text = errorDescription,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Spacer(Modifier.height(20.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                onClick = onRetry
            )
            if (showChangeSourceButton) {
                Spacer(Modifier.height(12.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.BalanceSyncError_ButtonChangeSource),
                    onClick = onChangeSource
                )
            }
            Spacer(Modifier.height(12.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BalanceSyncError_ButtonReport),
                onClick = onReport
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncError403Preview() {
    SyncErrorContent(
        coinCode = "ETH",
        errorDescription = annotatedStringResource(R.string.source_blocked_by_provider_error),
        showChangeSourceButton = true,
        onClose = {},
        onRetry = {},
        onChangeSource = {},
        onReport = {},
    )
}
