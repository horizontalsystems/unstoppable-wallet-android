package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R

@Immutable
class Typography internal constructor(
    val title1: TextStyle,
    val title2М: TextStyle,
    val title2R: TextStyle,
    val title3: TextStyle,
    val headline1: TextStyle,
    val headline2: TextStyle,
    val body: TextStyle,
    val bodyR: TextStyle,
    val bodyItalic: TextStyle,
    val subhead: TextStyle,
    val subheadR: TextStyle,
    val subheadB: TextStyle,
    val subheadSB: TextStyle,
    val caption: TextStyle,
    val captionSB: TextStyle,
    val micro: TextStyle,
    val microSB: TextStyle,
) {


    constructor(
        defaultFontFamily: FontFamily = manropeFont,
        title1: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 38.sp,
            letterSpacing = 0.sp,
        ),
        title2М: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 36.sp,
            letterSpacing = 0.sp,
        ),
        title2R: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            letterSpacing = 0.sp,
        ),
        title3: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
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
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        bodyR: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.sp,
        ),
        bodyItalic: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.sp,
        ),
        subhead: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
        subheadR: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
        subheadB: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 0.sp,
        ),
        subheadSB: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
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
        title2М = title2М.withFontFamily(defaultFontFamily),
        title2R = title2R.withFontFamily(defaultFontFamily),
        title3 = title3.withFontFamily(defaultFontFamily),
        headline1 = headline1.withFontFamily(defaultFontFamily),
        headline2 = headline2.withFontFamily(defaultFontFamily),
        body = body.withFontFamily(defaultFontFamily),
        bodyR = body.withFontFamily(defaultFontFamily),
        bodyItalic = bodyItalic.withFontFamily(defaultFontFamily),
        subhead = subhead.withFontFamily(defaultFontFamily),
        subheadR = subheadR.withFontFamily(defaultFontFamily).copy(lineHeight = 20.sp),
        subheadB = subheadB.withFontFamily(defaultFontFamily),
        subheadSB = subheadSB.withFontFamily(defaultFontFamily),
        caption = caption.withFontFamily(defaultFontFamily),
        captionSB = captionSB.withFontFamily(defaultFontFamily),
        micro = micro.withFontFamily(defaultFontFamily),
        microSB = microSB.withFontFamily(defaultFontFamily),
    )

}

fun ColoredTextStyle(textStyle: TextStyle, color: Color, textAlign: TextAlign = TextAlign.Unspecified): TextStyle {
    return textStyle.copy(
        color = color,
        textAlign = textAlign
    )
}

private fun TextStyle.withFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

internal val LocalTypography = staticCompositionLocalOf { Typography() }

val manropeFont = FontFamily(
    Font(R.font.manrope_regular),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)