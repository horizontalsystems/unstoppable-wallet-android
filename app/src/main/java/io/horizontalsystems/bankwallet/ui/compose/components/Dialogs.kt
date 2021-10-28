package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem

@Composable
fun SelectorDialogCompose(
    title: Int,
    selectorItems: List<SelectorItem>,
    onSelect: ((Int) -> Unit),
    onDismiss: (() -> Unit)
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.padding(horizontal = 20.dp).height(IntrinsicSize.Min),
            shape = RoundedCornerShape(16.dp),
            color = ComposeAppTheme.colors.lawrence
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(title),
                        color = ComposeAppTheme.colors.grey,
                        style = ComposeAppTheme.typography.subhead1,
                    )
                }
                selectorItems.forEachIndexed { index, item ->
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clickable { onSelect.invoke(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Divider(
                            thickness = 1.dp,
                            color = ComposeAppTheme.colors.steel10,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        Text(
                            item.caption,
                            color = if (item.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.oz,
                            style = ComposeAppTheme.typography.body,
                        )
                    }
                }
            }
        }
    }
}
