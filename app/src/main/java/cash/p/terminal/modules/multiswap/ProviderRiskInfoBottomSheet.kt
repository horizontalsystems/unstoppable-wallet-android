package cash.p.terminal.modules.multiswap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.modules.multiswap.providers.ProviderRiskType
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProviderRiskInfoBottomSheet(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ProviderRiskInfoContent(onClose = dismiss)
    }
}

@Composable
private fun ProviderRiskInfoContent(onClose: () -> Unit) {
    BottomSheetHeader(
        iconPainter = painterResource(R.drawable.ic_info_24),
        iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
        title = stringResource(R.string.swap_provider_type_info_title),
        onCloseClick = onClose,
    ) {
        body_leah(
            text = stringResource(R.string.swap_provider_type_info_description),
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ProviderRiskType.entries.forEach { type ->
                ProviderRiskInfoRow(type)
            }
        }
        VSpacer(height = 20.dp)
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.Button_GotIt),
            onClick = onClose,
        )
        VSpacer(height = 32.dp)
    }
}

@Composable
private fun ProviderRiskInfoRow(type: ProviderRiskType) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgePill(
            iconRes = type.iconRes,
            text = stringResource(type.titleRes),
            contentColor = type.color(),
        )
        subhead2_grey(text = stringResource(type.descriptionRes))
    }
}

@Preview
@Composable
private fun ProviderRiskInfoContentPreview() {
    ComposeAppTheme {
        ProviderRiskInfoContent(onClose = {})
    }
}
