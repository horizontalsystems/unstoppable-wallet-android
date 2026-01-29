package io.horizontalsystems.bankwallet.modules.balance.ui

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

    // Handle size changes or external index changes to prevent jumping in infinite mode
    LaunchedEffect(itemsSize, initialIndex) {
        if (!lazyListState.isScrollInProgress) {
            val currentIndex = lazyListState.firstVisibleItemIndex
            val currentActualIndex = currentIndex % itemsSize
            if (currentActualIndex != initialIndex) {
                val targetIndex = if (isInfinite) {
                    currentIndex - currentActualIndex + initialIndex
                } else {
                    initialIndex
                }
                lazyListState.scrollToItem(targetIndex)
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
                    style = ComposeAppTheme.typography.title3,
                    color = if (isSelected) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.andy,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun WheelDatePicker(
    selectedDay: Int,
    selectedMonth: Int,
    selectedYear: Int,
    onDaySelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onYearSelected: (Int) -> Unit,
    years: List<Int> = (1900..2100).toList()
) {
    val months = DateFormatSymbols().months.filter { it.isNotEmpty() }
    val yearStrings = years.map { it.toString() }

    val daysInMonth = remember(selectedMonth, selectedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            onDaySelected(daysInMonth)
        }
    }

    val dayStrings = (1..daysInMonth).map { it.toString() }

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.weight(1f))
        WheelPicker(
            items = dayStrings,
            initialIndex = (selectedDay - 1).coerceIn(0, dayStrings.size - 1),
            onItemSelected = { onDaySelected(it + 1) },
            modifier = Modifier.weight(1f),
            isInfinite = true
        )
        WheelPicker(
            items = months,
            initialIndex = (selectedMonth - 1).coerceIn(0, 11),
            onItemSelected = { onMonthSelected(it + 1) },
            modifier = Modifier.weight(2.2f),
            isInfinite = true
        )
        WheelPicker(
            items = yearStrings,
            initialIndex = years.indexOf(selectedYear).coerceAtLeast(0),
            onItemSelected = { onYearSelected(years[it]) },
            modifier = Modifier.weight(1.5f),
            isInfinite = false
        )
        Box(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelDatePickerBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    onConfirm: (Int, Int, Int) -> Unit,
    loading: Boolean,
    initialDay: Int = Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
    initialMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    initialYear: Int = Calendar.getInstance().get(Calendar.YEAR)
) {
    var day by remember { mutableIntStateOf(initialDay) }
    var month by remember { mutableIntStateOf(initialMonth) }
    var year by remember { mutableIntStateOf(initialYear) }

    BottomSheetContent(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        buttons = {
            HSButton(
                title = stringResource(R.string.Button_Apply),
                loadingIndicator = loading,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                onClick = {
                    onConfirm(day, month, year)
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
        Box(modifier = Modifier.padding(vertical = 32.dp)) {
            WheelDatePicker(
                selectedDay = day,
                selectedMonth = month,
                selectedYear = year,
                onDaySelected = { day = it },
                onMonthSelected = { month = it },
                onYearSelected = { year = it }
            )
        }
    }
}
