package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

data class TabItem<T>(val title: String, val selected: Boolean, val item: T, val icon: ImageSource? = null, val label: String? = null)

@Composable
fun <T>Tabs(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    Column(modifier = Modifier.height(45.dp)) {
        TabRow(
            modifier = Modifier.padding(horizontal = 16.dp).height(44.dp),
            selectedTabIndex = selectedIndex,
            backgroundColor = ComposeAppTheme.colors.tyler,
            contentColor = ComposeAppTheme.colors.tyler,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    color = ComposeAppTheme.colors.jacob
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    modifier = Modifier.height(43.dp).padding(horizontal = 16.dp),
                    selected = tab.selected,
                    onClick = {
                        onClick.invoke(tab.item)
                    },
                    content = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead1
                        ) {
                            Text(
                                text = tab.title,
                                color = if (selectedIndex == index) ComposeAppTheme.colors.oz else ComposeAppTheme.colors.grey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    })
            }
        }
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
    }
}

@Composable
fun <T>ScrollableTabs(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    Column(modifier = Modifier.height(45.dp)) {
        ScrollableTabRow(
            modifier = Modifier.height(44.dp),
            selectedTabIndex = selectedIndex,
            backgroundColor = ComposeAppTheme.colors.tyler,
            contentColor = ComposeAppTheme.colors.tyler,
            edgePadding = 16.dp,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    color = ComposeAppTheme.colors.jacob
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    modifier = Modifier.height(43.dp),
                    selected = tab.selected,
                    onClick = {
                        onClick.invoke(tab.item)
                    },
                    content = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead1
                        ) {
                            Text(
                                text = tab.title,
                                color = if (tab.selected) ComposeAppTheme.colors.oz else ComposeAppTheme.colors.grey
                            )
                        }
                    }
                )
            }
        }
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
    }
}
