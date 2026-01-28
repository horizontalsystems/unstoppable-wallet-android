package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.text.DateFormatSymbols
import java.util.*

@Composable
fun WheelPicker(
    items: List<String>,
    initialIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isInfinite: Boolean = false
) {
    val itemsSize = items.size
    if (itemsSize == 0) return

    val totalItemsCount = if (isInfinite) Int.MAX_VALUE else itemsSize
    val startIndex = if (isInfinite) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % itemsSize) + initialIndex
    } else {
        initialIndex
    }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val currentIndex = lazyListState.firstVisibleItemIndex
            val actualIndex = currentIndex % itemsSize
            onItemSelected(actualIndex)
        }
    }

    Box(
        modifier = modifier.height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection Highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(ComposeAppTheme.colors.lawrence.copy(alpha = 0.5f))
        )

        LazyColumn(
            state = lazyListState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(vertical = 68.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(totalItemsCount) { index ->
                val actualIndex = index % itemsSize
                val isSelected = lazyListState.firstVisibleItemIndex == index
                Text(
                    text = items[actualIndex],
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .wrapContentHeight(),
                    textAlign = TextAlign.Center,
                    fontSize = if (isSelected) 20.sp else 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.grey,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun MonthYearPicker(
    selectedMonth: Int, // 1-12
    selectedYear: Int,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    years: List<Int> = (2000..2100).toList()
) {
    val months = DateFormatSymbols().months.filter { it.isNotEmpty() }
    val yearStrings = years.map { it.toString() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        WheelPicker(
            items = months,
            initialIndex = (selectedMonth - 1).coerceIn(0, 11),
            onItemSelected = { onMonthSelected(it + 1) },
            modifier = Modifier.weight(1f),
            isInfinite = true
        )
        WheelPicker(
            items = yearStrings,
            initialIndex = years.indexOf(selectedYear).coerceAtLeast(0),
            onItemSelected = { onYearSelected(years[it]) },
            modifier = Modifier.weight(1f),
            isInfinite = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    var month by remember { mutableIntStateOf(initialMonth) }
    var year by remember { mutableIntStateOf(initialYear) }

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(month, year) }) {
                Text("OK", color = ComposeAppTheme.colors.leah)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = ComposeAppTheme.colors.grey)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = ComposeAppTheme.colors.tyler
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Month & Year",
                style = ComposeAppTheme.typography.title3,
                color = ComposeAppTheme.colors.leah,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            MonthYearPicker(
                selectedMonth = month,
                selectedYear = year,
                onMonthSelected = { month = it },
                onYearSelected = { year = it }
            )
        }
    }
}
