package cash.p.terminal.modules.configuredtoken

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import cash.p.terminal.R
import cash.p.terminal.ui_compose.requireInput
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui.compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.BaseComposableBottomSheetFragment
import cash.p.terminal.ui_compose.BottomSheetHeaderMultiline
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui_compose.findNavController
import cash.p.terminal.wallet.Token
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.helpers.HudHelper

class ConfiguredTokenInfoDialog : BaseComposableBottomSheetFragment() {

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
                ConfiguredTokenInfo(navController, navController.requireInput<Token>())
            }
        }
    }
}

@Composable
private fun ConfiguredTokenInfo(navController: NavController, token: Token) {
    val viewModel = viewModel<ConfiguredTokenInfoViewModel>(factory = ConfiguredTokenInfoViewModel.Factory(token))
    val uiState = viewModel.uiState

    cash.p.terminal.ui_compose.theme.ComposeAppTheme {
        BottomSheetHeaderMultiline(
            iconPainter = uiState.iconSource.painter(),
            title = uiState.title,
            subtitle = uiState.subtitle,
            onCloseClick = { navController.popBackStack() }
        ) {
            when (val tokenInfoType = uiState.tokenInfoType) {
                is ConfiguredTokenInfoType.Contract -> {
                    ContractInfo(tokenInfoType)
                }

                ConfiguredTokenInfoType.Bch -> {
                    body_leah(
                        text = stringResource(id = R.string.ManageCoins_BchTypeDescription),
                        modifier = Modifier.padding(
                            start = 32.dp,
                            top = 12.dp,
                            end = 32.dp,
                            bottom = 24.dp
                        )
                    )
                }

                is ConfiguredTokenInfoType.Bips -> {
                    body_leah(
                        text = stringResource(
                            R.string.ManageCoins_BipsDescription,
                            tokenInfoType.blockchainName,
                            tokenInfoType.blockchainName,
                            tokenInfoType.blockchainName
                        ),
                        modifier = Modifier.padding(
                            start = 32.dp,
                            top = 12.dp,
                            end = 32.dp,
                            bottom = 24.dp
                        )
                    )
                }

                is ConfiguredTokenInfoType.BirthdayHeight -> {
                    CellUniversalLawrenceSection(showFrame = true) {
                        RowUniversal(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            val view = LocalView.current
                            val clipboardManager = LocalClipboardManager.current
                            body_leah(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.Restore_BirthdayHeight),
                            )
                            val birthdayHeight = tokenInfoType.height?.toString() ?: "---"
                            ButtonSecondaryDefault(
                                modifier = Modifier.padding(start = 16.dp),
                                title = birthdayHeight,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(birthdayHeight))
                                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                                }
                            )
                        }
                    }
                }

                null -> Unit
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ContractInfo(tokenInfoType: ConfiguredTokenInfoType.Contract) {
    val context = LocalContext.current

    InfoText(text = stringResource(id = R.string.ManageCoins_ContractAddress))
    CellUniversalLawrenceSection(showFrame = true) {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                painter = rememberAsyncImagePainterWithFallback(
                    model = tokenInfoType.platformImageUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_32)
                ),
                contentDescription = "platform"
            )
            HSpacer(16.dp)
            subhead2_leah(
                modifier = Modifier.weight(1f),
                text = tokenInfoType.reference,
            )

            tokenInfoType.explorerUrl?.let {
                HSpacer(16.dp)
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_globe_20,
                    contentDescription = stringResource(R.string.Button_Browser),
                    onClick = {
                        LinkHelper.openLinkInAppBrowser(context, it)
                    }
                )
            }
        }
    }

    VSpacer(24.dp)
}
