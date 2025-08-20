package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.components.HFillSpacer

@Composable
private fun Cell(
    paddingValues: PaddingValues,
    left: @Composable() (RowScope.() -> Unit)?,
    middle: @Composable() (RowScope.() -> Unit),
    right: @Composable() (RowScope.() -> Unit)?,
    onClick: (() -> Unit)?,
) {
    var modifier = Modifier
        .fillMaxWidth()

    onClick?.let {
        modifier = modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier.padding(paddingValues = paddingValues),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left?.invoke(this)
        middle.invoke(this)
        HFillSpacer(0.dp)
        right?.invoke(this)
    }
}

@Composable
fun CellPrimary(
    left: @Composable() (RowScope.() -> Unit)? = null,
    middle: @Composable() (RowScope.() -> Unit),
    right: @Composable() (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Cell(
        paddingValues = PaddingValues(16.dp),
        left = left,
        middle = middle,
        right = right,
        onClick = onClick
    )
}

@Composable
fun CellSecondary(
    left: @Composable() (RowScope.() -> Unit)? = null,
    middle: @Composable() (RowScope.() -> Unit),
    right: @Composable() (RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Cell(
        paddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        left = left,
        middle = middle,
        right = right,
        onClick = onClick
    )
}