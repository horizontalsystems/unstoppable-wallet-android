package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CellMultilineLawrenceSection(
    composableItems: List<@Composable () -> Unit>
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        composableItems.forEachIndexed { index, composable ->
            CellMultilineLawrence(borderTop = index != 0) {
                composable()
            }
        }
    }
}

@Composable
fun CellMultilineLawrenceSection(
    content: @Composable () -> Unit
) {
    CellMultilineLawrenceSection(listOf(content))
}

@Composable
fun <T> CellMultilineLawrenceSection(
    items: Iterable<T>,
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
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
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
    content: @Composable () -> Unit
) {
    CellSingleLineLawrenceSection(listOf(content))
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
fun HSSectionRounded(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        content()
    }
}

@Composable
fun Item(content: @Composable () -> Unit) {
    CellSingleLineLawrence(
        content = content
    )
}

@Composable
fun CellSingleLineLawrence(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    CellSingleLine(
        borderTop = borderTop,
        borderBottom = borderBottom,
        color = ComposeAppTheme.colors.lawrence,
        content = content
    )
}

@Composable
fun CellSingleLine(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    color: Color? = null,
    content: @Composable () -> Unit,
) {
    val colorModifier = when {
        color != null -> Modifier.background(color)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(colorModifier)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        content.invoke()
    }
}

@Composable
fun CellHeaderSorting(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
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
fun CellMultilineClear(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    height: Dp = 60.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val clickableModifier = when (onClick) {
        null -> Modifier
        else -> Modifier.clickable {
            onClick.invoke()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .then(clickableModifier)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        content.invoke()
    }
}

@Composable
fun CellSingleLineClear(
    modifier: Modifier = Modifier,
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
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        Row(
            modifier = modifier
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
        HsDivider()
        caption_grey(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
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

@Composable
fun RowUniversal(
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 12.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val clickableModifier = when (onClick) {
        null -> Modifier
        else -> Modifier.clickable {
            onClick.invoke()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 24.dp)
            .then(clickableModifier)
            .then(modifier)
            .padding(vertical = verticalPadding),
        verticalAlignment = verticalAlignment,
        content = content
    )
}

@Composable
fun CellUniversalLawrenceSection(
    content: @Composable () -> Unit
) {
    CellUniversalLawrenceSection(listOf(content))
}

@Composable
fun CellUniversalLawrenceSection(
    composableItems: List<@Composable () -> Unit>
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        composableItems.forEachIndexed { index, composable ->
            SectionUniversalItem(
                borderTop = index != 0,
            ) {
                composable()
            }
        }
    }
}

@Composable
fun <T> CellUniversalLawrenceSection(
    items: Iterable<T>,
    showFrame: Boolean = false,
    itemContent: @Composable (T) -> Unit
) {
    val frameModifier = if (showFrame) {
        Modifier.border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .then(frameModifier)
    ) {
        items.forEachIndexed { index, itemData ->
            SectionUniversalItem(
                borderTop = index != 0,
            ) {
                itemContent(itemData)
            }
        }
    }
}

@Composable
fun CellUniversalLawrenceSection(
    showFrame: Boolean = false,
    content: @Composable () -> Unit
) {
    val frameModifier = if (showFrame) {
        Modifier.border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .then(frameModifier)
    ) {
        SectionUniversalItem {
            content()
        }
    }
}

@Composable
fun <T> CellUniversalLawrenceSection(
    items: List<T>,
    showFrame: Boolean = false,
    limit: Int,
    itemContent: @Composable (T) -> Unit
) {
    val frameModifier = if (showFrame) {
        Modifier.border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .then(frameModifier)
    ) {
        val size = items.size
        items.subList(0, limit.coerceAtMost(size)).forEachIndexed { index, itemData ->
            SectionUniversalItem(
                borderTop = index != 0,
            ) {
                itemContent(itemData)
            }
        }

        if (size > limit) {
            var visible by remember { mutableStateOf(false) }
            AnimatedVisibility(visible = visible) {
                Column {
                    items.subList(limit, size).forEach { itemData ->
                        SectionUniversalItem(
                            borderTop = true,
                        ) {
                            itemContent(itemData)
                        }
                    }
                }
            }

            RowUniversal(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(),
                verticalPadding = 0.dp,
                onClick = {
                    visible = !visible
                }
            ) {
                val text = if (visible) {
                    stringResource(id = R.string.CoinPage_ShowLess)
                } else {
                    stringResource(id = R.string.CoinPage_ShowMore)
                }

                body_leah(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SectionUniversalItem(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable () -> Unit,
) {

    //content items should use RowUniversal

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        content.invoke()
    }

}

@Composable
fun SectionItemBorderedRowUniversalClear(
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Column {
        if (borderTop) {
            HsDivider()
        }
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = onClick,
            content = content
        )
        if (borderBottom) {
            HsDivider()
        }
    }
}

@Composable
fun CellBorderedRowUniversal(
    modifier: Modifier = Modifier,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    backgroundColor: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        RowUniversal(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            content = content
        )
    }
}
