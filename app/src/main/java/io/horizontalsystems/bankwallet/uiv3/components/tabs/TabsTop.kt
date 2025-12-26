package io.horizontalsystems.bankwallet.uiv3.components.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun <T> TabsTop(
    type: TabsTopType,
    tabs: List<TabItem<T>>,
    onClick: (T) -> Unit
) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    val premiumIndexes = buildList {
        tabs.forEachIndexed { index, item ->
            if (item.premium) {
                add(index)
            }
        }
    }

    TabsTop(
        type = type,
        items = tabs.map { it.title },
        selectedIndex = selectedIndex,
        onSelect = {
            onClick.invoke(tabs[it].item)
        },
        premiumIndexes = premiumIndexes,
    )
}

enum class TabsTopType {
    Scrolled, Fitted
}

data class TabItem<T>(
    val title: String,
    val selected: Boolean,
    val item: T,
    val premium: Boolean = false,
)

@Composable
fun TabsTop(
    type: TabsTopType,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    premiumIndexes: List<Int> = listOf()
) {
    val tabs = @Composable {
        items.forEachIndexed { index, item ->
            val selected = selectedIndex == index
            val premium = premiumIndexes.contains(index)
            Tab(
                modifier = Modifier.height(52.dp),
                selected = selected,
                onClick = { onSelect(index) },
                text = {
                    val style = if (selected) {
                        ComposeAppTheme.typography.subheadSB
                    } else {
                        ComposeAppTheme.typography.subhead
                    }

                    val color = if (selected) {
                        ComposeAppTheme.colors.leah
                    } else if (premium) {
                        ComposeAppTheme.colors.jacob
                    } else {
                        ComposeAppTheme.colors.grey
                    }

                    Text(
                        text = item,
                        color = color,
                        style = style,
                        maxLines = 1
                    )
                },
            )
        }
    }

    when (type) {
        TabsTopType.Scrolled -> {
            SecondaryScrollableTabRow(
                containerColor = ComposeAppTheme.colors.tyler,
                selectedTabIndex = selectedIndex,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = false),
                        height = 2.dp,
                        color = ComposeAppTheme.colors.jacob
                    )
                },
                divider = {
                },
                edgePadding = 16.dp,
                minTabWidth = 0.dp,
                tabs = tabs
            )
        }
        TabsTopType.Fitted -> {
            SecondaryTabRow(
                modifier = Modifier.background(ComposeAppTheme.colors.tyler)
                    .padding(horizontal = 16.dp)
                ,
                containerColor = ComposeAppTheme.colors.tyler,
                selectedTabIndex = selectedIndex,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = false),
                        height = 2.dp,
                        color = ComposeAppTheme.colors.jacob
                    )
                },
                divider = {
                },
                tabs = tabs
            )
        }
    }
}

@Preview
@Composable
fun Preview_TabsTopScrolled() {
    ComposeAppTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var selectedTabIndex by remember { mutableIntStateOf(1) }
            TabsTop(
                type = TabsTopType.Scrolled,
                items = listOf("All") +  List(10) { index ->
                    "Tab $index"
                },
                selectedIndex = selectedTabIndex,
                onSelect = {
                    selectedTabIndex = it
                },
                premiumIndexes = listOf(3),
            )
        }
    }
}

@Preview
@Composable
fun Preview_TabsTopFitted() {
    ComposeAppTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var selectedTabIndex by remember { mutableIntStateOf(1) }
            TabsTop(
                type = TabsTopType.Fitted,
                items = listOf("All") +  List(3) { index ->
                    "Tab $index"
                },
                selectedIndex = selectedTabIndex,
                onSelect = {
                    selectedTabIndex = it
                },
                premiumIndexes = listOf(3),
            )
        }
    }
}
