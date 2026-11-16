package io.horizontalsystems.walletkit.modules.walletconnect.session.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.components.ButtonStyle
import io.horizontalsystems.designkit.components.ButtonVariant
import io.horizontalsystems.designkit.components.HSDropdownButton
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.subhead1_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey

@Composable
fun DropDownCell(
    title: String,
    value: String,
    enabled: Boolean,
    testTag: String? = null,
    onSelect: () -> Unit,
) {
    RowUniversal(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(enabled = enabled, onClick = onSelect),
        onClick = if (enabled) onSelect else null
    ) {
        subhead2_grey(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        if (enabled) {
            HSDropdownButton(
                modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
                variant = ButtonVariant.Secondary,
                style = ButtonStyle.Transparent,
                title = value,
                onClick = onSelect,
            )
        } else {
            subhead1_leah(
                text = value,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}
