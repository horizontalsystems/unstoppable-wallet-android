@file:Suppress("PackageNaming")

package cash.p.terminal.modules.transactions.poison_status

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import cash.p.terminal.ui_compose.annotatedStringResource
import cash.p.terminal.ui_compose.components.ButtonPrimaryRed
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

enum class SuspiciousAddressAction {
    COPY,
    ADD_TO_CONTACTS,
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopyWarningBottomSheet(
    onCopyAnyway: () -> Unit,
    onDismiss: () -> Unit,
    action: SuspiciousAddressAction = SuspiciousAddressAction.COPY,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        sheetState = sheetState,
        containerColor = ComposeAppTheme.colors.transparent,
    ) {
        BottomSheetHeader(
            iconPainter = painterResource(R.drawable.ic_attention_24),
            iconTint = ColorFilter.tint(ComposeAppTheme.colors.jacob),
            title = stringResource(R.string.copy_warning_title),
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ComposeAppTheme.colors.jacob, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                subhead2_leah(stringResource(R.string.copy_warning_body_1))
                VSpacer(12.dp)
                val body2Res = when (action) {
                    SuspiciousAddressAction.COPY -> R.string.copy_warning_body_2
                    SuspiciousAddressAction.ADD_TO_CONTACTS -> R.string.poison_warning_add_body_2
                }
                subhead2_leah(annotatedStringResource(body2Res))
                VSpacer(12.dp)
                val body3Res = when (action) {
                    SuspiciousAddressAction.COPY -> R.string.copy_warning_body_3
                    SuspiciousAddressAction.ADD_TO_CONTACTS -> R.string.poison_warning_verify_before_adding
                }
                subhead2_leah(annotatedStringResource(body3Res))
            }
            Spacer(Modifier.height(24.dp))
            ButtonPrimaryRed(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(
                    when (action) {
                        SuspiciousAddressAction.COPY -> R.string.copy_warning_copy_anyway
                        SuspiciousAddressAction.ADD_TO_CONTACTS -> R.string.poison_warning_add_anyway
                    }
                ),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onCopyAnyway()
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            ButtonPrimaryTransparent(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(
                    when (action) {
                        SuspiciousAddressAction.COPY -> R.string.copy_warning_dont_copy
                        SuspiciousAddressAction.ADD_TO_CONTACTS -> R.string.poison_warning_dont_add
                    }
                ),
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onDismiss()
                    }
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Suppress("UnusedPrivateMember")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CopyWarningBottomSheetPreview() {
    ComposeAppTheme {
        CopyWarningBottomSheet(
            onCopyAnyway = {},
            onDismiss = {},
        )
    }
}
