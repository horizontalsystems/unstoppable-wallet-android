package io.horizontalsystems.bankwallet.modules.syncerror

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.evmnetwork.EvmNetworkModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeader
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class SyncErrorDialog : BaseComposableBottomSheetFragment() {
    private val error by lazy {
        requireArguments().getString(errorKey) ?: ""
    }

    private val wallet by lazy {
        requireArguments().getParcelable<Wallet>(walletKey)
    }

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
                wallet?.let {
                    SyncErrorScreen(findNavController(), it, error)
                }
            }
        }
    }

    companion object {
        private const val walletKey = "walletKey"
        private const val errorKey = "errorKey"

        fun prepareParams(wallet: Wallet, errorMessage: String?) = bundleOf(
            walletKey to wallet,
            errorKey to errorMessage,
        )
    }
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
            subtitle = wallet.coin.name,
            onCloseClick = { navController.popBackStack() }
        ) {
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Text(
                text = stringResource(R.string.BalanceSyncError_ReportButtonExplanation),
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10
            )
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.BalanceSyncError_ButtonRetry),
                onClick = {
                    viewModel.retry()
                    navController.popBackStack()
                }
            )
            viewModel.sourceType?.let { sourceType ->
                Spacer(Modifier.height(16.dp))
                ButtonPrimaryDefault(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.BalanceSyncError_ButtonChangeSource),
                    onClick = {
                        navController.popBackStack()

                        when (sourceType) {
                            is SyncErrorViewModel.SourceType.EvmNetworkSettings -> {
                                navController.slideFromRight(
                                    R.id.evmNetworkFragment,
                                    EvmNetworkModule.args(sourceType.blockchain, sourceType.account)
                                )
                            }
                            SyncErrorViewModel.SourceType.PrivacySettings -> {
                                navController.slideFromRight(
                                    R.id.mainFragment_to_privacySettingsFragment
                                )
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
            ButtonPrimaryDefault(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
            Spacer(Modifier.height(16.dp))
        }
    }
}
