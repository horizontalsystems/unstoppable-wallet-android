package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellRightInfo(
    eyebrow: HSString? = null,
    title: HSString,
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

        Text(
            text = title.text,
            style = ComposeAppTheme.typography.headline2,
            color = title.color ?: ComposeAppTheme.colors.leah,
        )

        subtitle?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subhead,
                color = it.color ?: ComposeAppTheme.colors.grey,
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
