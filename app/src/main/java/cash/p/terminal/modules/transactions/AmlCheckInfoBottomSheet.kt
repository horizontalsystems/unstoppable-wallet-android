package cash.p.terminal.modules.transactions

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.body_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmlCheckInfoBottomSheet(
    onPremiumSettingsClick: () -> Unit,
    onLaterClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_star_filled_20),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.alpha_aml_title),
            titleColor = ComposeAppTheme.colors.leah,
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                body_leah(text = stringResource(R.string.aml_check_info_description))
            }
            Spacer(Modifier.height(8.dp))
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = stringResource(R.string.premium_settings),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onPremiumSettingsClick()
                    }
                }
            )
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = stringResource(R.string.aml_check_info_later),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onLaterClick()
                    }
                }
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AmlCheckInfoBottomSheetPreview() {
    ComposeAppTheme {
        AmlCheckInfoBottomSheet(
            onPremiumSettingsClick = {},
            onLaterClick = {},
            onDismiss = {}
        )
    }
}
