package io.horizontalsystems.bankwallet.modules.syncerror

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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryTransparent
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
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

    ComposeAppTheme {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_red_24),
            title = stringResource(R.string.BalanceSyncError_Title),
            onCloseClick = { navController.popBackStack() }
        ) {

            Spacer(Modifier.height(32.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                onClick = {
                    viewModel.retry()
                    navController.popBackStack()
                }
            )
            if (viewModel.sourceChangeable) {
                Spacer(Modifier.height(12.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.BalanceSyncError_ButtonChangeSource),
                    onClick = {
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
                                navController.slideFromBottom(R.id.evmNetworkFragment, blockchainWrapper.blockchain)
                            }
                            else -> {}
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.BalanceSyncError_ButtonReport),
                onClick = {
                    navController.popBackStack()

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(viewModel.reportEmail))
                        putExtra(Intent.EXTRA_TEXT, error)
                    }

                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        clipboardManager.setText(AnnotatedString(viewModel.reportEmail))
                        HudHelper.showSuccessMessage(view, R.string.Hud_Text_EmailAddressCopied)
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

