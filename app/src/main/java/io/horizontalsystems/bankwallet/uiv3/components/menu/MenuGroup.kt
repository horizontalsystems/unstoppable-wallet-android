package io.horizontalsystems.bankwallet.uiv3.components.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.subheadSB_grey
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered

data class MenuItemX<T>(
    val title: String,
    val selected: Boolean,
    val item: T
)

@Composable
fun <T> MenuGroup(
    title: String?,
    items: List<MenuItemX<T>>,
    onDismissRequest: () -> Unit,
    onSelectItem: (T) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .width(240.dp)
                .padding(vertical = 8.dp)
        ) {
            title?.let {
                MenuHeader(title)
            }

            items.forEach { item ->
                BoxBordered(top = true) {
                    MenuItem(
                        item = item,
                        onSelectItem = {
                            onSelectItem.invoke(it)
                            onDismissRequest.invoke()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> MenuItem(
    item: MenuItemX<T>,
    onSelectItem: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !item.selected,
                onClick = {
                    onSelectItem.invoke(item.item)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = item.title,
            style = ComposeAppTheme.typography.body,
            color = if (item.selected) ComposeAppTheme.colors.jacob else ComposeAppTheme.colors.leah
        )
    }
}

@Composable
private fun MenuHeader(title: String) {
    Box(modifier = Modifier.height(40.dp)) {
        subheadSB_grey(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            text = title,
            textAlign = TextAlign.Center
        )
    }
}