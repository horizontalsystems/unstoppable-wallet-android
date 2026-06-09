package cash.p.terminal.modules.configuredtoken

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui.helpers.LinkHelper
import cash.p.terminal.ui_compose.BottomSheetHeaderMultiline
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.ButtonSecondaryDefault
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HudHelper
import cash.p.terminal.ui_compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.wallet.Token
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguredTokenInfoBottomSheet(
    token: Token,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val viewModel = koinViewModel<ConfiguredTokenInfoViewModel>(
        key = token.tokenQuery.id
    ) {
        parametersOf(token)
    }
    val uiState = viewModel.uiState

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BottomSheetHeaderMultiline(
            iconPainter = uiState.iconSource.painter(),
            title = uiState.title,
            subtitle = uiState.subtitle,
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
            }
        ) {
            ConfiguredTokenInfoContent(uiState.tokenInfoType)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ConfiguredTokenInfoContent(tokenInfoType: ConfiguredTokenInfoType?) {
    when (tokenInfoType) {
        is ConfiguredTokenInfoType.Contract -> ContractInfo(tokenInfoType)
        ConfiguredTokenInfoType.Bch -> BchInfo()
        is ConfiguredTokenInfoType.Bips -> BipsInfo(tokenInfoType)
        is ConfiguredTokenInfoType.BirthdayHeight -> BirthdayHeightInfo(tokenInfoType)
        null -> Unit
    }
}

@Composable
private fun BchInfo() {
    body_leah(
        text = stringResource(id = R.string.ManageCoins_BchTypeDescription),
        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
    )
}

@Composable
private fun BipsInfo(tokenInfoType: ConfiguredTokenInfoType.Bips) {
    val descriptionRes = if (tokenInfoType.blockchainType == BlockchainType.Litecoin) {
        R.string.manage_coins_bips_description_litecoin
    } else {
        R.string.ManageCoins_BipsDescription
    }
    body_leah(
        text = stringResource(
            descriptionRes,
            tokenInfoType.blockchainName,
            tokenInfoType.blockchainName,
            tokenInfoType.blockchainName
        ),
        modifier = Modifier.padding(start = 32.dp, top = 12.dp, end = 32.dp, bottom = 24.dp)
    )
}

@Composable
private fun BirthdayHeightInfo(tokenInfoType: ConfiguredTokenInfoType.BirthdayHeight) {
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
