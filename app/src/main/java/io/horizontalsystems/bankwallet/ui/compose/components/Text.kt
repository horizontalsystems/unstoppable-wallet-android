package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun headline1_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline1,
        color = ComposeAppTheme.colors.grey,
    )
}

@Composable
fun headline1_bran(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline1,
        color = ComposeAppTheme.colors.bran,
    )
}

@Composable
fun body_bran(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.bran,
    )
}

@Composable
fun L2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline1,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun headline1_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    L2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun L5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline1,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun headline1_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    L5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun headline2_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun headline2_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun headline2_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun headline2_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun headline2_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun headline2_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun headline2_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun headline2_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun headline2_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun A10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.headline2,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun headline2_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    A10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun body_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun body_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun body_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun body_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun body_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun body_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun body_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun body_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun body_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun B10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.body,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun body_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    B10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun subhead1_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun subhead1_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun subhead1_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun subhead1_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun subhead1_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun subhead1_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun subhead1_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun subhead1_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun subhead1_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun C10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun subhead1_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    C10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun subhead2_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun subhead2_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun subhead2_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun subhead2_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun subhead2_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun subhead2_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun subhead2_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun subhead2_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun subhead2_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun D10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead2,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun subhead2_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    D10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun micro_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.micro,
        color = ComposeAppTheme.colors.leah,
    )
}

@Composable
fun micro_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.micro,
        color = ComposeAppTheme.colors.grey,
    )
}

@Composable
fun E1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun captionSB_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun captionSB_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun captionSB_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun captionSB_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun captionSB_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun captionSB_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun captionSB_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun captionSB_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun captionSB_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun E10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.captionSB,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun captionSB_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    E10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun caption_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun caption_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun caption_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun caption_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun caption_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun caption_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun caption_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun caption_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun caption_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun F10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.caption,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun caption_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    F10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.grey,
    )
}
@Composable
fun subhead1Italic_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun subhead1Italic_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun subhead1Italic_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun subhead1Italic_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun subhead1Italic_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun subhead1Italic_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun subhead1Italic_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun subhead1Italic_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun subhead1Italic_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun G10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.subhead1Italic,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun subhead1Italic_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    G10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H1(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.grey,
    )
}

@Composable
fun title2_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title2,
        color = ComposeAppTheme.colors.leah,
    )
}

@Composable
fun title2R_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title2R,
        color = ComposeAppTheme.colors.grey,
    )
}

@Composable
fun title3_grey(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H1(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H2(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.leah,
    )
}
@Composable
fun title3_leah(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H2(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H3(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.jacob,
    )
}
@Composable
fun title3_jacob(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H3(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H4(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.remus,
    )
}
@Composable
fun title3_remus(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H4(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H5(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.lucian,
    )
}
@Composable
fun title3_lucian(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H5(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H6(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.issykBlue,
    )
}
@Composable
fun title3_issykBlue(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H6(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H7(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.grey50,
    )
}
@Composable
fun title3_grey50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H7(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H8(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.yellow50,
    )
}
@Composable
fun title3_yellow50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H8(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H9(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.green50,
    )
}
@Composable
fun title3_green50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H9(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}

@Composable
fun H10(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
        style = ComposeAppTheme.typography.title3,
        color = ComposeAppTheme.colors.red50,
    )
}
@Composable
fun title3_red50(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    H10(
        text = text,
        modifier = modifier,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        onTextLayout = onTextLayout,
    )
}
