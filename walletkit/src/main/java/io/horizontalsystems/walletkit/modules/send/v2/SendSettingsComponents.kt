package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSDropdownButton
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
import io.horizontalsystems.walletkit.uiv3.components.cell.CellPrimary

/**
 * A filled, rounded card of chain send settings rows on the page background, with a
 * hairline between the rows.
 */
@Composable
fun SendSettingsCard(rows: List<@Composable () -> Unit>) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = 8.dp),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HsDivider(modifier = Modifier.fillMaxWidth())
            }
            row()
        }
    }
}

/**
 * A settings row: a title over its description, with the current [value] in a dropdown
 * button on the right. A [warning] continues the description in its own colour.
 */
@Composable
fun SendSettingsRow(
    title: String,
    subtitle: String,
    value: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    warning: String? = null,
) {
    CellPrimary(
        middle = {
            Column {
                Text(
                    text = title,
                    style = ComposeAppTheme.typography.headline2,
                    color = ComposeAppTheme.colors.leah,
                )
                Text(
                    text = buildAnnotatedString {
                        append(subtitle)
                        if (warning != null) {
                            append(" ")
                            withStyle(SpanStyle(color = ComposeAppTheme.colors.jacob)) {
                                append(warning)
                            }
                        }
                    },
                    style = ComposeAppTheme.typography.subhead,
                    color = ComposeAppTheme.colors.grey,
                )
            }
        },
        right = {
            HSDropdownButton(
                modifier = Modifier.testTag(testTag),
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                title = value,
                enabled = enabled,
                onClick = onClick,
            )
        },
        onClick = if (enabled) onClick else null,
    )
}
