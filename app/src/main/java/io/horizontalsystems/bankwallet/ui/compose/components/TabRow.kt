package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Orange

data class TabItem<T>(
    val title: String,
    val selected: Boolean,
    val item: T,
    val icon: ImageSource? = null,
    val label: String? = null,
    val enabled: Boolean = true,
    val premium: Boolean = false,
)

@Composable
fun <T>Tabs(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    Box(
        modifier = Modifier
            .background(ComposeAppTheme.colors.tyler)
            .height(44.dp)
    ) {
        HsDivider(modifier = Modifier.align(Alignment.BottomCenter))

        TabRow(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .height(44.dp),
            selectedTabIndex = selectedIndex,
            backgroundColor = ComposeAppTheme.colors.transparent,
            contentColor = ComposeAppTheme.colors.tyler,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    color = Orange
                )
            }
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    selected = tab.selected,
                    onClick = {
                        onClick.invoke(tab.item)
                    },
                    content = {
                        Text(
                            text = tab.title,
                            color = if (selectedIndex == index) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                            style = if (selectedIndex == index) ComposeAppTheme.typography.subheadB else ComposeAppTheme.typography.subheadSB,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    })
            }
        }
    }
}

@Composable
fun <T>ScrollableTabs(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    Box(
        modifier = Modifier
            .background(ComposeAppTheme.colors.tyler)
            .height(44.dp)
    ) {
        HsDivider(modifier = Modifier.align(Alignment.BottomCenter))

        ScrollableTabRow(
            selectedTabIndex = selectedIndex,
            backgroundColor = ComposeAppTheme.colors.transparent,
            contentColor = ComposeAppTheme.colors.tyler,
            edgePadding = 16.dp,
            indicator = @Composable { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)),
                    color = Orange
                )
            }
        ) {
            tabs.forEach { tab ->
                val textColor = when{
                    tab.selected -> ComposeAppTheme.colors.leah
                    tab.premium -> Orange
                    else -> ComposeAppTheme.colors.grey
                }
                Tab(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    selected = tab.selected,
                    onClick = {
                        onClick.invoke(tab.item)
                    },
                    content = {
                        ProvideTextStyle(
                            ComposeAppTheme.typography.subhead
                        ) {
                            Text(
                                text = tab.title,
                                color = textColor,
                                style = if (tab.selected) ComposeAppTheme.typography.subheadB else ComposeAppTheme.typography.subheadSB,
                            )
                        }
                    }
                )
            }
        }
    }
}
