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

@Composable
fun TabsTopScrolled(items: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
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

                    Text(
                        text = item,
                        maxLines = 1,
                        style = style
                    )
                },
                selectedContentColor = ComposeAppTheme.colors.leah,
                unselectedContentColor = ComposeAppTheme.colors.grey,
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
                }
            )
        }
    }
}
