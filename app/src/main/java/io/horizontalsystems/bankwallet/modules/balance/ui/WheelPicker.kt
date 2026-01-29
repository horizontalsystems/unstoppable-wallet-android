package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.bankwallet.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.bankwallet.uiv3.components.controls.HSButton
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock
import java.text.DateFormatSymbols
import java.util.Calendar

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
    val baseSnapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)
    val snapFlingBehavior = remember(baseSnapFlingBehavior) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return with(baseSnapFlingBehavior) {
                    performFling(initialVelocity * 0.6f)
                }
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val currentIndex = lazyListState.firstVisibleItemIndex
            val actualIndex = currentIndex % itemsSize
            onItemSelected(actualIndex)
        }
    }

    Box(
        modifier = modifier.height(132.dp),
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
            contentPadding = PaddingValues(vertical = 44.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthYearPickerBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    var month by remember { mutableIntStateOf(initialMonth) }
    var year by remember { mutableIntStateOf(initialYear) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BottomSheetContent(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = "OK",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onConfirm(month, year)
                    onDismissRequest()
                }
            )
        }
    ) {
        BottomSheetHeaderV3(
            title = stringResource(R.string.BirthdayDatePicker_Title),
            onCloseClick = onDismissRequest
        )
        TextBlock(
            text = stringResource(R.string.BirthdayDatePicker_Description),
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.border(1.dp, Color.Red).padding(vertical = 32.dp)) {
            MonthYearPicker(
                selectedMonth = month,
                selectedYear = year,
                onMonthSelected = { month = it },
                onYearSelected = { year = it }
            )
        }
    }
}
