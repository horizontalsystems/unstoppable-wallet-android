package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun <T> SelectorDialogCompose(
    title: String? = null,
    items: List<TabItem<T>>,
    onDismissRequest: () -> Unit,
    onSelectItem: (T) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
        ) {
            title?.let {
                Box(modifier = Modifier.height(40.dp)) {
                    subhead1_grey(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        text = title,
                        textAlign = TextAlign.Center
                    )
                }
            }

            items.forEach {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clickable(
                            enabled = !it.selected,
                            onClick = {
                                onSelectItem.invoke(it.item)
                                onDismissRequest.invoke()
                            }
                        )
                ) {
                    Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
                    val color = if (it.selected) {
                        ComposeAppTheme.colors.jacob
                    } else {
                        ComposeAppTheme.colors.leah
                    }
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        text = it.title,
                        style = ComposeAppTheme.typography.body,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
