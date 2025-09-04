package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.Badge

@Composable
fun CellMiddleInfo(
    eyebrow: HSString? = null,
    title: HSString,
    badge: HSString? = null,
    subtitleBadge: HSString? = null,
    subtitle: HSString? = null,
    subtitle2: HSString? = null,
    description: HSString? = null,
) {
    Column {
        eyebrow?.let {
            Text(
                text = it.text,
                style = ComposeAppTheme.typography.subhead,
                color = it.color ?: ComposeAppTheme.colors.grey,
            )
        }

        Row(
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.text,
                style = ComposeAppTheme.typography.headline2,
                color = title.color ?: ComposeAppTheme.colors.leah,
            )

            badge?.let {
                Badge(text = it.text)
            }
        }

        Row(
            horizontalArrangement = spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            subtitleBadge?.let {
                Badge(text = it.text)
            }
            Row(horizontalArrangement = spacedBy(4.dp)) {
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
                subtitle2?.let {
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
            }
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
