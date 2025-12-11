package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellRightControlsButtonTextTextGroup(
    title: HSString? = null,
    subtitle: HSString? = null,
    description: HSString? = null
) {
    Column(horizontalAlignment = Alignment.End) {
        title?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.headline2,
                color = it.color ?: ComposeAppTheme.colors.leah,
            )
        }
        subtitle?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subheadSB,
                color = it.color ?: ComposeAppTheme.colors.leah,
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
