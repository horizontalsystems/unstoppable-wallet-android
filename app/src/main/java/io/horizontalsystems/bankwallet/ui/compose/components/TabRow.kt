package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Grey
import io.horizontalsystems.bankwallet.ui.compose.Steel10

@Composable
fun Tabs(tabs: List<String>, selectedIndex: Int, onClick: (Int) -> Unit) {

    var tabIndex by remember { mutableStateOf(selectedIndex) }

    Column(modifier = Modifier.height(45.dp)) {
        TabRow(
            modifier = Modifier.height(44.dp),
            selectedTabIndex = tabIndex,
            backgroundColor = ComposeAppTheme.colors.tyler,
            contentColor = ComposeAppTheme.colors.tyler,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = ComposeAppTheme.colors.jacob
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = tabIndex == index,
                    onClick = {
                        tabIndex = index
                        onClick.invoke(index)
                    },
                    text = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead1.copy(
                                textAlign = TextAlign.Center
                            )
                        ) {
                            Text(
                                text = tab,
                                color = if (tabIndex == index) ComposeAppTheme.colors.oz else Grey
                            )
                        }
                    })
            }
        }
        Divider(thickness = 1.dp, color = Steel10)
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
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = ComposeAppTheme.colors.jacob
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = tabIndex == index,
                    onClick = {
                        tabIndex = index
                        onClick.invoke(index)
                    },
                    text = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead1.copy(
                                textAlign = TextAlign.Center
                            )
                        ) {
                            Text(
                                text = tab,
                                color = if (tabIndex == index) ComposeAppTheme.colors.oz else Grey
                            )
                        }
                    }
                )
            }
        }
        Divider(thickness = 1.dp, color = Steel10)
    }
}
