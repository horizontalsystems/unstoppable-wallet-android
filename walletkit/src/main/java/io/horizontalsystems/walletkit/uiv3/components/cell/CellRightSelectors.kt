package io.horizontalsystems.walletkit.uiv3.components.cell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.uiv3.components.controls.FilledCheckMark

@Composable
fun CellRightSelectors(
    title: HSString? = null,
    subtitle: HSString? = null,
    description1: HSString? = null,
    description2: HSString? = null,
    selected: Boolean? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.End
        ) {
            title?.let {
                Text(
                    text = it.text,
                    style = ComposeAppTheme.typography.headline2,
                    color = it.color ?: ComposeAppTheme.colors.leah,
                    textAlign = TextAlign.End,
                )
            }
            subtitle?.let {
                Text(
                    text = it.text,
                    style = ComposeAppTheme.typography.subheadSB,
                    color = it.color ?: ComposeAppTheme.colors.leah,
                    textAlign = TextAlign.End,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                description1?.let {
                    Text(
                        text = it.text,
                        style = ComposeAppTheme.typography.captionSB,
                        color = it.color ?: ComposeAppTheme.colors.grey,
                        textAlign = TextAlign.End,
                    )
                }
                description2?.let {
                    Text(
                        text = it.text,
                        style = ComposeAppTheme.typography.captionSB,
                        color = it.color ?: ComposeAppTheme.colors.grey,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
        selected?.let {
            if (it) {
                FilledCheckMark(size = 20.dp)
            } else {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.selector_unchecked_20),
                    contentDescription = null,
                    tint = ComposeAppTheme.colors.grey
                )
            }
        }
    }
}

@Preview
@Composable
fun Prev_CellRightSelectors() {
    ComposeAppTheme {
        CellRightSelectors(
            title = "Text".hs,
            description1 = "Text".hs,
            description2 = "Text".hs(color = ComposeAppTheme.colors.lucian),
            selected = true
        )
    }
}
