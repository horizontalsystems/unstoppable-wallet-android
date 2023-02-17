package cash.p.terminal.modules.manageaccount.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.*

@Composable
fun ActionButton(title: Int, onClick: () -> Unit) {
    ButtonsGroupWithShade {
        ButtonPrimaryYellow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            title = stringResource(title),
            onClick = onClick,
        )
    }
}

@Composable
fun HidableContent(
    content: String,
    hideScreenText: String? = null,
) {
    var hidden by remember { mutableStateOf(hideScreenText != null) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, ComposeAppTheme.colors.steel20, RoundedCornerShape(24.dp))
            .clickable(enabled = hideScreenText != null, onClick = { hidden = !hidden })
    ) {

        D2(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            text = content
        )

        if (hideScreenText != null && hidden) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ComposeAppTheme.colors.tyler),
                contentAlignment = Alignment.Center
            ) {
                subhead2_grey(hideScreenText)
            }
        }
    }
}

@Composable
fun KeyActionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    CellUniversalLawrenceSection(
        listOf {
            RowUniversal(
                onClick = onClick
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                body_leah(
                    modifier = Modifier.weight(1f),
                    text = title,
                )
                Icon(
                    painter = painterResource(id = cash.p.terminal.R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        })
    InfoText(text = description)
    Spacer(Modifier.height(20.dp))
}
