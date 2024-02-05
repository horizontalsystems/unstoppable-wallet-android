package io.horizontalsystems.bankwallet.ui.compose.components.cell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellUniversal(
    borderTop: Boolean = true,
    paddingVertical: Dp = 12.dp,
    paddingHorizontal: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable() (RowScope.() -> Unit),
) {
    Box {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        val modifierClickable = if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

        Row(
            modifier = Modifier
                .then(modifierClickable)
                .padding(vertical = paddingVertical, horizontal = paddingHorizontal),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun CellUniversalFixedHeight(
    borderTop: Boolean = true,
    height: Dp,
    content: @Composable() (RowScope.() -> Unit),
) {
    Box {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        Row(
            modifier = Modifier.height(height),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun SectionUniversalLawrence(
    content: @Composable() (ColumnScope.() -> Unit),
) {
    SectionUniversal(
        backgroundColor = ComposeAppTheme.colors.lawrence,
        content = content
    )
}

@Composable
private fun SectionUniversal(
    backgroundColor: Color,
    content: @Composable() (ColumnScope.() -> Unit),
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor),
        content = content
    )
}
