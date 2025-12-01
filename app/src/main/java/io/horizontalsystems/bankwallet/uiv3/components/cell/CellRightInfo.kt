package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellRightInfo(
    eyebrow: HSString? = null,
    title: HSString? = null,
    titleSubheadSb: HSString? = null,
    subtitle: HSString? = null,
    description: HSString? = null,
) {
    Column(horizontalAlignment = Alignment.End) {
        eyebrow?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subhead,
                color = it.color ?: ComposeAppTheme.colors.grey,
            )
        }
        title?.let {
            Text(
                text = title.text,
                style = ComposeAppTheme.typography.headline2,
                color = when {
                    title.color != null -> title.color
                    title.dimmed -> ComposeAppTheme.colors.andy
                    else -> ComposeAppTheme.colors.leah
                },
            )
        }
        titleSubheadSb?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subheadSB,
                color = it.color ?: ComposeAppTheme.colors.leah,
            )
        }

        subtitle?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subhead,
                color = when {
                    it.color != null -> it.color
                    it.dimmed -> ComposeAppTheme.colors.andy
                    else -> ComposeAppTheme.colors.grey
                },
            )
        }

        description?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.captionSB,
                color = it.color ?: ComposeAppTheme.colors.grey,
            )
        }
    }
}
