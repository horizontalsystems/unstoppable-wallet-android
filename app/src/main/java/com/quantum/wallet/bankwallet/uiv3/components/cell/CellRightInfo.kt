package com.quantum.wallet.bankwallet.uiv3.components.cell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellRightInfo(
    eyebrow: HSString? = null,
    title: HSString? = null,
    titleSubheadSb: HSString? = null,
    subtitle: HSString? = null,
    description: HSString? = null,
    onClick: (() -> Unit)? = null,
) {
    var modifier: Modifier = Modifier

    onClick?.let {
        modifier = modifier.clickable(
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        eyebrow?.let {
            Text(
                text = it.text,
                textAlign = TextAlign.End,
                style = ComposeAppTheme.typography.subhead,
                color = it.color ?: ComposeAppTheme.colors.grey,
            )
        }
        title?.let {
            Text(
                text = title.text,
                textAlign = TextAlign.End,
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
                textAlign = TextAlign.End,
                style = ComposeAppTheme.typography.subheadSB,
                color = it.color ?: ComposeAppTheme.colors.leah,
            )
        }

        subtitle?.let {
            Text(
                text = it.text,
                textAlign = TextAlign.End,
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
                textAlign = TextAlign.End,
                style = ComposeAppTheme.typography.captionSB,
                color = it.color ?: ComposeAppTheme.colors.grey,
            )
        }
    }
}
