package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.BoxTyler44
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondary
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle

@Composable
fun SuggestionsBar(
    modifier: Modifier = Modifier,
    percents: List<Int> = listOf(25, 50, 75, 100),
    onDelete: () -> Unit,
    onSelect: (Int) -> Unit,
    selectEnabled: Boolean,
    deleteEnabled: Boolean,
) {
    Box(modifier = modifier) {
        BoxTyler44(borderTop = true) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                percents.forEach { percent ->
                    ButtonSecondary(
                        enabled = selectEnabled,
                        onClick = { onSelect.invoke(percent) }
                    ) {
                        Text(
                            text = "$percent%",
                            modifier = modifier,
                            style = ComposeAppTheme.typography.subhead1,
                            color = if (selectEnabled) {
                                ComposeAppTheme.colors.leah
                            } else {
                                ComposeAppTheme.colors.grey50
                            },
                        )
                    }
                }
                ButtonSecondaryCircle(
                    icon = R.drawable.ic_delete_20,
                    enabled = deleteEnabled,
                    tint = if (deleteEnabled) {
                        ComposeAppTheme.colors.leah
                    } else {
                        ComposeAppTheme.colors.grey50
                    },
                    onClick = onDelete
                )
            }
        }
    }
}
