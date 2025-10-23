package io.horizontalsystems.bankwallet.uiv3.components.bottomsheet

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subhead_grey
import io.horizontalsystems.bankwallet.uiv3.components.AlertCard
import io.horizontalsystems.bankwallet.uiv3.components.AlertFormat
import io.horizontalsystems.bankwallet.uiv3.components.AlertType
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfoTextIcon
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellSecondary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonSize
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonStyle
import io.horizontalsystems.bankwallet.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    warningFrameSlot: (@Composable () -> Unit)? = null,
    cellsGroupSlot: (@Composable ColumnScope.() -> Unit)? = null,
    buttons: (@Composable ColumnScope.() -> Unit)? = null,
    infoText: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { }
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.warning_filled_24),
            title = "Confirm"
        )
        subhead_grey(
            text = "dapp.uniswap.org",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        BottomSheetTextBlock("This action will change your receive address format for Bitcoin in Unstoppable app. After that, the app will resync itself with Bitcoin blockchain.")
        warningFrameSlot?.let {
            Box(modifier = Modifier.padding(16.dp)) {
                warningFrameSlot()
            }
        }
        cellsGroupSlot?.let {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .border(1.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
                    .padding(vertical = 8.dp)
            ) {
                cellsGroupSlot()
            }
        }
        infoText?.let {
            TextBlock(text = infoText)
        }
        buttons?.let {
            ButtonsStack {
                buttons()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetContent(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    buttons: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = ComposeAppTheme.colors.lawrence,
        dragHandle = { }
    ) {
        content()
        buttons?.let {
            ButtonsStack {
                buttons()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun Preview_BottomSheetContent() {
    ComposeAppTheme {
        BottomSheetContent(
            onDismissRequest = {},
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            buttons = {
                HSButton(
                    title = "Connect",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {  }
                )
                HSButton(
                    title = "Cancel",
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Transparent,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    onClick = {  }
                )
            },
            content = {
                BottomSheetHeaderV3(
                    image72 = painterResource(R.drawable.warning_filled_24),
                    title = "Title"
                )
                TextBlock(
                    text = "By clicking connect, you allow this app to view your public address.",
                    textAlign = TextAlign.Center
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun Preview_BottomSheet() {
    ComposeAppTheme {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(Unit) {
            sheetState.show()
        }
        BottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            warningFrameSlot = {
                AlertCard(
                    format = AlertFormat.Inline,
                    type = AlertType.Critical,
                    text = "This website is not verified and may lead to phishing or loss of funds. Connecting is strongly discouraged.",
                )
            },
            cellsGroupSlot = {
                repeat(5) {
                    CellSecondary(
                        middle = {
                            CellMiddleInfo(eyebrow = "Subhead".hs)
                        },
                        right = {
                            CellRightInfoTextIcon(
                                text = "SubheadSB".hs
                            )
                        }
                    )
                }
            },
            infoText = "By clicking connect, you allow this app to view your public address.",
            buttons = {
                HSButton(
                    title = "Connect",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {  }
                )
                HSButton(
                    title = "Cancel",
                    modifier = Modifier.fillMaxWidth(),
                    style = ButtonStyle.Transparent,
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Medium,
                    onClick = {  }
                )
            }

        )
    }
}
