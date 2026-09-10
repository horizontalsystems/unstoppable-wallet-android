package io.horizontalsystems.walletkit.uiv3.components.tabs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

/** A tab in [TabsFolder]: a title with an optional leading icon. */
data class TabFolderItem(
    val title: String,
    @DrawableRes val icon: Int? = null,
)

/**
 * Folder-style tabs: the selected tab is a rounded "folder" flap in the surface color joined
 * to the content below, the others are plain labels on the page background.
 */
@Composable
fun TabsFolder(
    tabs: List<TabFolderItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(ComposeAppTheme.colors.tyler)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEachIndexed { index, tab ->
            FolderTab(
                item = tab,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        }
    }
}

@Composable
private fun FolderTab(
    item: TabFolderItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fill = ComposeAppTheme.colors.lawrence
    val contentColor = if (selected) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey
    val textStyle = if (selected) ComposeAppTheme.typography.subheadSB else ComposeAppTheme.typography.subhead

    Row(
        modifier = Modifier
            .height(43.dp)
            .then(
                if (selected) {
                    Modifier.drawBehind { drawPath(folderTabPath(size, density), fill) }
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(
                start = if (selected) 16.dp else 12.dp,
                end = if (selected) 28.dp else 12.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item.icon?.let { icon ->
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(icon),
                contentDescription = null,
                tint = contentColor,
            )
        }
        Text(
            text = item.title,
            color = contentColor,
            style = textStyle,
            maxLines = 1,
        )
    }
}

/**
 * The selected tab's outline: an 8dp rounded top-left corner, a flat top, and a 22dp wide
 * slanted right edge that curves off the top and runs down to the bottom-right corner.
 */
private fun folderTabPath(size: Size, density: Float): Path {
    val w = size.width
    val h = size.height
    val radius = 8f * density
    val slantStart = w - 22f * density

    return Path().apply {
        moveTo(0f, radius)
        arcTo(Rect(0f, 0f, radius * 2, radius * 2), 180f, 90f, false)
        lineTo(slantStart + 0.89f * density, 0f)
        cubicTo(
            slantStart + 6.276f * density, 0f,
            slantStart + 11.001f * density, 3.587f * density,
            slantStart + 12.449f * density, 8.774f * density,
        )
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
}

@Preview
@Composable
private fun Preview_TabsFolder() {
    ComposeAppTheme(darkTheme = false) {
        var selected by remember { mutableIntStateOf(0) }
        Column {
            TabsFolder(
                tabs = listOf(
                    TabFolderItem("Standard"),
                    TabFolderItem("Private", icon = R.drawable.ic_incognito_24),
                    TabFolderItem("CrossPay"),
                ),
                selectedIndex = selected,
                onSelect = { selected = it },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.White)
            )
        }
    }
}
