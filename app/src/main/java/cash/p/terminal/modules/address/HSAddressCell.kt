package cash.p.terminal.modules.address

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import io.horizontalsystems.chartview.cell.CellUniversal

@Composable
fun HSAddressCell(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    val borderColor = ComposeAppTheme.colors.transparent

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence),
        content = {
            CellUniversal(
                borderTop = false,
                onClick = onClick
            ) {
                subhead2_grey(text = title)

                HSpacer(16.dp)
                subhead1_leah(
                    modifier = Modifier.weight(1f),
                    text = value
                )

                HSpacer(16.dp)
                Icon(
                    painter = painterResource(id = R.drawable.ic_down_arrow_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    )
}