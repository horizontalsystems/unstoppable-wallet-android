package io.horizontalsystems.bankwallet.uiv3.components.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SecondaryScrollableTabRow
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
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem

@Composable
fun <T>TabsTopScrolled(tabs: List<TabItem<T>>, onClick: (T) -> Unit) {
    val selectedIndex = tabs.indexOfFirst { it.selected }

    val premiumIndexes = buildList {
        tabs.forEachIndexed { index, item ->
            if (item.premium) {
                add(index)
            }
        }
    }

    TabsTopScrolled(
        items = tabs.map { it.title },
        selectedIndex = selectedIndex,
        onSelect = {
            onClick.invoke(tabs[it].item)
        },
        premiumIndexes = premiumIndexes
    )
}

@Composable
fun TabsTopScrolled(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, premiumIndexes: List<Int> = listOf()) {
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
        minTabWidth = 0.dp
    ) {
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
}

@Preview
@Composable
fun Preview_TabsTop() {
    ComposeAppTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(12.dp)) {
            var selectedTabIndex by remember { mutableIntStateOf(1) }
            TabsTopScrolled(
                items = listOf("All") +  List(10) { index ->
                    "Tab $index"
                },
                selectedIndex = selectedTabIndex,
                onSelect = {
                    selectedTabIndex = it
                },
                premiumIndexes = listOf(3)
            )
        }
    }
}
