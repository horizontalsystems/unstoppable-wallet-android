package io.horizontalsystems.bankwallet.modules.configuredtoken

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
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ConfiguredToken
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.BaseComposableBottomSheetFragment
import io.horizontalsystems.bankwallet.ui.extensions.BottomSheetHeaderMultiline
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.core.findNavController
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
                val configuredToken = arguments?.getParcelable<ConfiguredToken>(configuredTokenKey)
                if (configuredToken != null) {
                    ConfiguredTokenInfo(findNavController(), configuredToken)
                }
            }
        }
    }

    companion object {
        private const val configuredTokenKey = "configuredToken"

        fun prepareParams(configuredToken: ConfiguredToken): Bundle {
            return bundleOf(configuredTokenKey to configuredToken)
        }
    }
}

@Composable
private fun ConfiguredTokenInfo(navController: NavController, configuredToken: ConfiguredToken) {
    val viewModel = viewModel<ConfiguredTokenInfoViewModel>(factory = ConfiguredTokenInfoViewModel.Factory(configuredToken))
    val uiState = viewModel.uiState

    ComposeAppTheme {
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
                        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
                    )
                }
                is ConfiguredTokenInfoType.Bips -> {
                    body_leah(
                        text = stringResource(R.string.ManageCoins_BipsDescription, tokenInfoType.blockchainName, tokenInfoType.blockchainName, tokenInfoType.blockchainName),
                        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
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
                painter = rememberAsyncImagePainter(
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
