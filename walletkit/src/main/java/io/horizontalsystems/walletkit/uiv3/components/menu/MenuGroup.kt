package io.horizontalsystems.walletkit.uiv3.components.menu

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.Dialog
import io.horizontalsystems.designkit.components.HSMenuGroup
import io.horizontalsystems.designkit.components.HSMenuGroupContent
import io.horizontalsystems.designkit.components.HSMenuGroupDefaults
import io.horizontalsystems.designkit.components.HSMenuItem
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.Select
import io.horizontalsystems.walletkit.ui.compose.WithTranslatableTitle
import io.horizontalsystems.walletkit.uiv3.exposeTestTags

data class MenuItemX<T>(
    val title: String,
    val selected: Boolean,
    val item: T,
    val enabled: Boolean = true,
    val icon: Painter? = null,
)

/**
 * Shows a designkit menu in a dialog. Choosing an item closes the dialog;
 * choosing the one that is already selected closes it without reporting a
 * selection.
 */
@Composable
fun <T> MenuGroup(
    title: String?,
    items: List<MenuItemX<T>>,
    onDismissRequest: () -> Unit,
    onSelectItem: (T) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        HSMenuGroup(
            modifier = Modifier.exposeTestTags(),
            title = title,
            items = items.map { item ->
                HSMenuItem(
                    title = item.title,
                    icon = item.icon,
                    selected = item.selected,
                    enabled = item.enabled,
                    onClick = {
                        if (!item.selected) {
                            onSelectItem.invoke(item.item)
                        }
                        onDismissRequest.invoke()
                    }
                )
            }
        )
    }
}

/**
 * The same dialog for a [Select]: one item per option, titled by the option,
 * with the selected one accented.
 */
@Composable
fun <T : WithTranslatableTitle> MenuGroup(
    title: String,
    select: Select<T>,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    MenuGroup(
        title = title,
        items = select.options.map {
            MenuItemX(it.title.getString(), it == select.selected, it)
        },
        onDismissRequest = onDismiss,
        onSelectItem = onSelect,
    )
}

/**
 * Shows a designkit menu as a popup anchored to the layout it is declared
 * in, moved by [offset]: the context menu of a row or the menu behind a top
 * bar icon. The items close it themselves, in their onClick.
 */
@Composable
fun MenuGroupPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<HSMenuItem>,
    title: String? = null,
    offset: DpOffset = DpOffset.Zero,
) {
    // Material's menu supplies the popup window, its placement and the
    // vertical padding, which equals HSMenuGroupDefaults.VerticalPadding.
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = HSMenuGroupDefaults.Shape,
        containerColor = ComposeAppTheme.colors.lawrence,
        // the popup has its own window, so tags must be exposed again inside it
        modifier = Modifier
            .width(HSMenuGroupDefaults.Width)
            .exposeTestTags()
    ) {
        HSMenuGroupContent(title = title, items = items)
    }
}
