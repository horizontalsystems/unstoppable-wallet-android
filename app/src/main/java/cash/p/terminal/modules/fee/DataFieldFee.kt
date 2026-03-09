package cash.p.terminal.modules.fee

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.InfoBottomSheet
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
fun DataFieldFee(
    primary: String,
    secondary: String,
    borderTop: Boolean = false,
    loading: Boolean = false,
    title: String = stringResource(id = R.string.fee),
) {
    val infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
    var showInfoDialog by remember { mutableStateOf(false) }

    QuoteInfoRow(
        borderTop = borderTop,
        title = {
            subhead2_grey(text = title)

            Image(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(
                        onClick = { showInfoDialog = true },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ),
                painter = painterResource(id = R.drawable.ic_info_20),
                contentDescription = ""
            )

        },
        value = {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = ComposeAppTheme.colors.grey,
                    strokeWidth = 2.dp
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    subhead2_leah(text = primary)
                    if (secondary.isNotEmpty()) {
                        VSpacer(height = 1.dp)
                        subhead2_grey(text = secondary)
                    }
                }
            }
        }
    )

    if (showInfoDialog) {
        InfoBottomSheet(
            title = title,
            text = infoText,
            onDismiss = { showInfoDialog = false }
        )
    }
}
