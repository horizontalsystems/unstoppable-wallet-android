package io.horizontalsystems.bankwallet.uiv3.components.cell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
private fun Cell(
    paddingValues: PaddingValues,
    left: @Composable() (() -> Unit)?,
    middle: @Composable() (() -> Unit),
    right: @Composable() (() -> Unit)?,
    backgroundColor: Color?,
    onClick: (() -> Unit)?,
) {
    var modifier = Modifier
        .fillMaxWidth()

    onClick?.let {
        modifier = modifier.clickable(onClick = onClick)
    }

    backgroundColor?.let {
        modifier = modifier.background(backgroundColor)
    }

    Row(
        modifier = modifier.padding(paddingValues = paddingValues),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left?.invoke()
        Box(modifier = Modifier.weight(1f)) {
            middle.invoke()
        }
        right?.let { rightContent ->
            Box(
                modifier = Modifier.widthIn(max = 200.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                rightContent()
            }
        }
    }
}

@Composable
fun CellPrimary(
    left: @Composable() (() -> Unit)? = null,
    middle: @Composable() (() -> Unit),
    right: @Composable() (() -> Unit)? = null,
    backgroundColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    Cell(
        paddingValues = PaddingValues(16.dp),
        left = left,
        middle = middle,
        right = right,
        backgroundColor = backgroundColor,
        onClick = onClick
    )
}

@Composable
fun CellSecondary(
    left: @Composable() (() -> Unit)? = null,
    middle: @Composable() (() -> Unit),
    right: @Composable() (() -> Unit)? = null,
    backgroundColor: Color? = null,
    onClick: (() -> Unit)? = null
) {
    Cell(
        paddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        left = left,
        middle = middle,
        right = right,
        onClick = onClick,
        backgroundColor = backgroundColor
    )
}

@Preview
@Composable
fun Preview_CellPrimary() {
    ComposeAppTheme {
        Column {
            CellPrimary(
                left = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_app_logo_72),
                        contentDescription = null
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = "The quick brown fox jumps over the lazy dog".hs
                    )
                },
                right = {
                    CellRightInfo(
                        title = "Value".hs
                    )
                }
            )

            Divider()

            CellPrimary(
                left = {
                    Image(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_app_logo_72),
                        contentDescription = null
                    )
                },
                middle = {
                    CellMiddleInfo(
                        title = "Title".hs
                    )
                },
                right = {
                    CellRightInfo(
                        title = "Value".hs
                    )
                }
            )
        }
    }
}