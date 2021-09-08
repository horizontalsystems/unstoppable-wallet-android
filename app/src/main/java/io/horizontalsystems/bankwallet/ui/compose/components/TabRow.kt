package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

data class TabItem<T>(val title: String, val selected: Boolean, val item: T)

@Composable
fun <T>Tabs(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    Column(modifier = Modifier.height(45.dp)) {
        TabRow(
            modifier = Modifier.height(44.dp),
            selectedTabIndex = selectedIndex,
            backgroundColor = ComposeAppTheme.colors.tyler,
            contentColor = ComposeAppTheme.colors.tyler,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
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
fun ScrollableTabs(tabs: List<String>, selectedIndex: Int, onClick: (Int) -> Unit) {
    var tabIndex by remember { mutableStateOf(selectedIndex) }

    Column(modifier = Modifier.height(45.dp)) {
        ScrollableTabRow(
            modifier = Modifier.height(44.dp),
            selectedTabIndex = tabIndex,
            backgroundColor = ComposeAppTheme.colors.tyler,
            contentColor = ComposeAppTheme.colors.tyler,
            edgePadding = 0.dp,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = ComposeAppTheme.colors.jacob
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    modifier = Modifier.height(43.dp),
                    selected = tabIndex == index,
                    onClick = {
                        tabIndex = index
                        onClick.invoke(index)
                    },
                    content = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead1
                        ) {
                            Text(
                                text = tab,
                                color = if (tabIndex == index) ComposeAppTheme.colors.oz else ComposeAppTheme.colors.grey
                            )
                        }
                    }
                )
            }
        }
        Divider(thickness = 1.dp, color = ComposeAppTheme.colors.steel10)
    }
}
