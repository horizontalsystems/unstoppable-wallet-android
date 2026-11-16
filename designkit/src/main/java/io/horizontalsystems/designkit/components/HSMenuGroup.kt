package io.horizontalsystems.designkit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.R
import io.horizontalsystems.designkit.theme.ComposeAppTheme

/**
 * One row of a [HSMenuGroup]. A [selected] item is accented and can still be
 * pressed; a disabled one is dimmed and does not call [onClick]. The
 * [modifier] goes on the row, for a test tag.
 */
@Immutable
class HSMenuItem(
    val title: String,
    val modifier: Modifier = Modifier,
    val description: String? = null,
    val icon: Painter? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

object HSMenuGroupDefaults {
    val Width = 240.dp
    val Shape = RoundedCornerShape(16.dp)
    val VerticalPadding = 8.dp
}

/**
 * The surface of a context menu: an optional [title] over a list of items
 * separated by dividers. It only draws the menu; showing it in a dialog or
 * a popup and closing it after a choice is up to the caller.
 */
@Composable
fun HSMenuGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    items: List<HSMenuItem>,
) {
    Column(
        modifier = modifier
            .width(HSMenuGroupDefaults.Width)
            .clip(HSMenuGroupDefaults.Shape)
            .background(ComposeAppTheme.colors.lawrence)
            .padding(vertical = HSMenuGroupDefaults.VerticalPadding)
            // A menu taller than the space it is given scrolls inside its
            // surface, so the last items stay reachable.
            .verticalScroll(rememberScrollState())
    ) {
        HSMenuGroupContent(title = title, items = items)
    }
}

/**
 * The inside of a [HSMenuGroup]: the title, the items and the dividers. For
 * a container that draws the menu surface itself, such as a popup anchored
 * to a control. Such a container takes its width, shape and vertical padding
 * from [HSMenuGroupDefaults] and uses Lawrence as its colour.
 */
@Composable
fun HSMenuGroupContent(
    title: String? = null,
    items: List<HSMenuItem>,
) {
    title?.let {
        MenuHeader(it)
    }

    items.forEachIndexed { index, item ->
        if (index > 0 || title != null) {
            Divider(
                thickness = 0.5.dp,
                color = ComposeAppTheme.colors.blade,
            )
        }
        MenuItems(item)
    }
}

@Composable
private fun MenuHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            style = ComposeAppTheme.typography.subheadSB,
            color = ComposeAppTheme.colors.grey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MenuItems(item: HSMenuItem) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val colors = ComposeAppTheme.colors
    val contentColor = when {
        !item.enabled -> colors.andy
        item.selected -> colors.orange
        else -> colors.leah
    }
    val titleStyle = when {
        item.enabled && item.selected -> ComposeAppTheme.typography.headline2
        else -> ComposeAppTheme.typography.body
    }

    Row(
        modifier = item.modifier
            .fillMaxWidth()
            .background(if (pressed) colors.blade else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = item.enabled,
                role = Role.Button,
                onClick = item.onClick,
            )
            .semantics { selected = item.selected }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item.icon?.let {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = it,
                contentDescription = null,
                tint = contentColor,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = titleStyle,
                color = contentColor,
            )
            item.description?.let {
                Text(
                    text = it,
                    style = ComposeAppTheme.typography.caption,
                    color = colors.grey,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview_HSMenuGroup() {
    ComponentPreview {
        HSMenuGroup(
            title = "Title",
            items = listOf(
                HSMenuItem(title = "Item", selected = true, onClick = {}),
                HSMenuItem(title = "Item", onClick = {}),
                HSMenuItem(title = "Item", description = "Description", onClick = {}),
                HSMenuItem(title = "Item", enabled = false, onClick = {}),
                HSMenuItem(title = "Item", selected = true, enabled = false, onClick = {}),
            )
        )
        val icon = painterResource(R.drawable.arrow_s_down_24)
        HSMenuGroup(
            items = listOf(
                HSMenuItem(title = "Item", icon = icon, onClick = {}),
                HSMenuItem(title = "Item", icon = icon, selected = true, onClick = {}),
                HSMenuItem(title = "Item", icon = icon, enabled = false, onClick = {}),
            )
        )
    }
}
