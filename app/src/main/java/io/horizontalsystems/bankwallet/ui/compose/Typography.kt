package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class Typography internal constructor(
    val title1: TextStyle,
    val title2: TextStyle,
    val title2R: TextStyle,
    val title3: TextStyle,
    val headline1: TextStyle,
    val headline2: TextStyle,
    val body: TextStyle,
    val bodyItalic: TextStyle,
    val subhead1: TextStyle,
    val subhead2: TextStyle,
    val subheadItalic: TextStyle,
    val caption: TextStyle,
    val captionSB: TextStyle,
    val micro: TextStyle,
    val microSB: TextStyle,
) {

    constructor(
        defaultFontFamily: FontFamily = FontFamily.Default,
        title1: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 38.sp,
            letterSpacing = 0.sp,
        ),
        title2: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.sp,
        ),
        title2R: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            letterSpacing = 0.sp,
        ),
        title3: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = 0.sp,
        ),
        headline1: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            letterSpacing = 0.sp,
        ),
        headline2: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        body: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        bodyItalic: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.sp,
        ),
        subhead1: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
        subhead2: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
        subheadItalic: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.sp,
        ),
        caption: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        ),
        captionSB: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.sp,
        ),
        micro: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            letterSpacing = 0.sp,
        ),
        microSB: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.sp,
        ),
    ) : this(
        title1 = title1.withFontFamily(defaultFontFamily),
        title2 = title2.withFontFamily(defaultFontFamily),
        title2R = title2R.withFontFamily(defaultFontFamily),
        title3 = title3.withFontFamily(defaultFontFamily),
        headline1 = headline1.withFontFamily(defaultFontFamily),
        headline2 = headline2.withFontFamily(defaultFontFamily),
        body = body.withFontFamily(defaultFontFamily),
        bodyItalic = bodyItalic.withFontFamily(defaultFontFamily),
        subhead1 = subhead1.withFontFamily(defaultFontFamily),
        subhead2 = subhead2.withFontFamily(defaultFontFamily),
        subheadItalic = subheadItalic.withFontFamily(defaultFontFamily),
        caption = caption.withFontFamily(defaultFontFamily),
        captionSB = captionSB.withFontFamily(defaultFontFamily),
        micro = micro.withFontFamily(defaultFontFamily),
        microSB = microSB.withFontFamily(defaultFontFamily),
    )

}

fun ColoredTextStyle(textStyle: TextStyle, color: Color): TextStyle {
    return TextStyle(
        color = color,
        fontWeight = textStyle.fontWeight,
        fontSize = textStyle.fontSize,
        fontStyle = textStyle.fontStyle
    )
}

private fun TextStyle.withFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

internal val LocalTypography = staticCompositionLocalOf { Typography() }