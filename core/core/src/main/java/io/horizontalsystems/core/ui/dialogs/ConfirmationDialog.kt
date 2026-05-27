package io.horizontalsystems.core.ui.dialogs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.ui_compose.BottomSheetHeader
import cash.p.terminal.ui_compose.R
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationDialogBottomSheet(
    title: String,
    icon: Int?,
    warningTitle: String?,
    warningText: String?,
    actionButtonTitle: String?,
    transparentButtonTitle: String?,
    onCloseClick: () -> Unit,
    onActionButtonClick: () -> Unit,
    onTransparentButtonClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCloseClick,
        dragHandle = null,
        sheetState = sheetState,
        containerColor = ComposeAppTheme.colors.transparent,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        ConfirmationDialog(
            title = title,
            icon = icon,
            warningTitle = warningTitle,
            warningText = warningText,
            actionButtonTitle = actionButtonTitle,
            transparentButtonTitle = transparentButtonTitle,
            onCloseClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onCloseClick()
                }
            },
            onActionButtonClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onActionButtonClick()
                }
            },
            onTransparentButtonClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    onTransparentButtonClick()
                }
            }
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    icon: Int?,
    warningTitle: String?,
    warningText: String?,
    actionButtonTitle: String?,
    transparentButtonTitle: String?,
    onCloseClick: () -> Unit,
    onActionButtonClick: () -> Unit,
    onTransparentButtonClick: () -> Unit,
) {
    BottomSheetHeader(
        iconPainter = painterResource(icon ?: R.drawable.ic_attention_24),
        iconTint = ColorFilter.Companion.tint(ComposeAppTheme.colors.jacob),
        title = title,
        onCloseClick = onCloseClick
    ) {

        warningText?.let {
            TextImportantWarning(
                title = warningTitle,
                modifier = Modifier.Companion.padding(horizontal = 16.dp, vertical = 12.dp),
                text = it
            )
            Spacer(Modifier.Companion.height(8.dp))
        }
        actionButtonTitle?.let {
            ButtonPrimaryYellow(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = actionButtonTitle,
                onClick = onActionButtonClick
            )
        }
        transparentButtonTitle?.let {
            ButtonPrimaryTransparent(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp),
                title = transparentButtonTitle,
                onClick = onTransparentButtonClick
            )
        }
        Spacer(Modifier.Companion.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmationDialogPreview() {
    ComposeAppTheme {
        ConfirmationDialog(
            title = "Preview title",
            icon = null,
            warningTitle = "Warning title",
            warningText = "Warning text",
            actionButtonTitle = "Confirm",
            transparentButtonTitle = "Cancel",
            onCloseClick = {},
            onActionButtonClick = {},
            onTransparentButtonClick = {}
        )
    }
}
