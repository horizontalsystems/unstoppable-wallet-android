package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun <T>CardTabs(
    tabItems: List<TabItem<T>>,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowPadding,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onClick: (T?) -> Unit,
) {
    val tabIndex = tabItems.indexOfFirst { it.selected }
    ComposeAppTheme(darkTheme) {
        ScrollableTabRow(
            selectedTabIndex = tabIndex,
            modifier = Modifier
                .padding(vertical = 12.dp)
                .height(94.dp)
            ,
            backgroundColor = Color.Transparent,
            edgePadding = edgePadding,
            indicator = {},
            divider = {}
        ) {
            tabItems.forEachIndexed { index, tabItem ->
                val selected = tabIndex == index
                val lastElement = index == tabItems.lastIndex
                Tab(
                    selected = selected,
                    onClick = {
                        onClick(if (selected) null else tabItem.item)
                    },
                    modifier = Modifier.padding(end = if (lastElement) 0.dp else 12.dp)
                ) {
                    val modifier = if (selected) {
                        Modifier.border(1.dp, ComposeAppTheme.colors.jacob, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }

                    Column(
                        modifier = modifier
                            .width(98.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ComposeAppTheme.colors.lawrence)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (tabItem.iconResId != null) {
                            Image(
                                painter = painterResource(id = tabItem.iconResId),
                                contentDescription = ""
                            )
                        }
                        Text(
                            text = tabItem.title,
                            style = ComposeAppTheme.typography.subhead1,
                            color = ComposeAppTheme.colors.oz
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun PreviewCardTabs() {
    var selectedIndex by remember { mutableStateOf(0) }
    val map = mapOf(
        "BTC" to R.drawable.coin_placeholder,
        "ETH" to R.drawable.coin_placeholder,
        "BNB" to R.drawable.coin_placeholder,
        "ZCASH" to R.drawable.coin_placeholder
    )

    val tabItems = map.toList().mapIndexed { index, (title, icon) ->
        TabItem(title, selectedIndex == index, title, icon)
    }

    CardTabs(
        tabItems = tabItems,
        edgePadding = 0.dp,
        darkTheme = true,
    ) { selected ->
        selectedIndex = tabItems.indexOfFirst { it.title == selected }
    }
}
