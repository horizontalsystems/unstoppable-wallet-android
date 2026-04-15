@file:Suppress("PackageNaming")

package cash.p.terminal.modules.transactions.poison_status

import android.content.res.Configuration
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
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.TransparentModalBottomSheet
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.caption_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionStatusesInfoSheet(
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    TransparentModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_info_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.grey),
            title = stringResource(R.string.transaction_statuses_title),
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            ) {
                subhead2_leah(
                    text = stringResource(R.string.transaction_statuses_description),
                )
                VSpacer(24.dp)

                PoisonStatus.entries.forEach { status ->
                    StatusEntry(status)
                    VSpacer(16.dp)
                }

                subhead2_leah(
                    text = stringResource(R.string.transaction_statuses_footer),
                )
            }
            VSpacer(24.dp)
            ButtonPrimaryYellow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.Button_GotIt),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                },
            )
            VSpacer(32.dp)
        }
    }
}

@Composable
private fun StatusEntry(status: PoisonStatus) {
    PoisonStatusInfoEntry(
        status = status,
        titleContent = { subhead2_leah(text = it) },
        descriptionContent = { caption_grey(text = it) },
    )
}

@Suppress("UnusedPrivateMember")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransactionStatusesInfoSheetPreview() {
    ComposeAppTheme {
        TransactionStatusesInfoSheet(onDismiss = {})
    }
}
