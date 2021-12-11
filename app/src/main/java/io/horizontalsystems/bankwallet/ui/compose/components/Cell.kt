package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun <T> CellMultilineLawrenceSection(
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        items.forEachIndexed { index, marketDataLine ->
            CellMultilineLawrence(borderTop = index != 0) {
                itemContent(marketDataLine)
            }
        }
    }
}

@Composable
fun CellMultilineLawrence(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        content.invoke()
    }
}

@Composable
fun <T> CellSingleLineLawrenceSection(
    items: List<T>,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        items.forEachIndexed { index, marketDataLine ->
            CellSingleLineLawrence(borderTop = index != 0) {
                itemContent(marketDataLine)
            }
        }
    }

}

@Composable
fun CellSingleLineLawrenceSection(
    composableItems: List<@Composable () -> Unit>
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        composableItems.forEachIndexed { index, composable ->
            CellSingleLineLawrence(borderTop = index != 0) {
                composable()
            }
        }
    }

}

@Composable
fun CellSingleLineLawrence(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        content.invoke()
    }
}


@Composable
fun CellData2(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        content.invoke()
    }

}

@Composable
fun CellSingleLineClear(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (borderTop) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}


@Composable
fun CellFooter(text: String) {
    Box(
        modifier = Modifier
            .height(58.dp)
            .fillMaxWidth(),
    ) {
        Divider(color = ComposeAppTheme.colors.steel10)
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            style = ComposeAppTheme.typography.caption,
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
fun CellFooterPreview() {
    ComposeAppTheme {
        CellFooter(text = stringResource(id = R.string.Market_PoweredByApi))
    }
}
