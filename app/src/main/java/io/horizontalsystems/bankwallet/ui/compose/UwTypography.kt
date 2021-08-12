package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
class UnTypography internal constructor(
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
        ),
        title2: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
        ),
        title2R: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
        ),
        title3: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        ),
        headline1: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
        ),
        headline2: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        ),
        body: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
        ),
        bodyItalic: TextStyle = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic
        ),
        subhead1: TextStyle = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        ),
        subhead2: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
        ),
        caption: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
        ),
        captionSB: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        ),
        micro: TextStyle = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
        ),
        microSB: TextStyle = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
        ),
    ) : this(
        title1 = title1.withDefaultFontFamily(defaultFontFamily),
        title2 = title2.withDefaultFontFamily(defaultFontFamily),
        title2R = title2R.withDefaultFontFamily(defaultFontFamily),
        title3 = title3.withDefaultFontFamily(defaultFontFamily),
        headline1 = headline1.withDefaultFontFamily(defaultFontFamily),
        headline2 = headline2.withDefaultFontFamily(defaultFontFamily),
        body = body.withDefaultFontFamily(defaultFontFamily),
        bodyItalic = bodyItalic.withDefaultFontFamily(defaultFontFamily),
        subhead1 = subhead1.withDefaultFontFamily(defaultFontFamily),
        subhead2 = subhead2.withDefaultFontFamily(defaultFontFamily),
        caption = caption.withDefaultFontFamily(defaultFontFamily),
        captionSB = captionSB.withDefaultFontFamily(defaultFontFamily),
        micro = micro.withDefaultFontFamily(defaultFontFamily),
        microSB = microSB.withDefaultFontFamily(defaultFontFamily),
    )

    fun copy(
        title1: TextStyle = this.title1,
        title2: TextStyle = this.title2,
        title2R: TextStyle = this.title2R,
        title3: TextStyle = this.title3,
        headline1: TextStyle = this.headline1,
        headline2: TextStyle = this.headline2,
        body: TextStyle = this.body,
        bodyItalic: TextStyle = this.bodyItalic,
        subhead1: TextStyle = this.subhead1,
        subhead2: TextStyle = this.subhead2,
        caption: TextStyle = this.caption,
        captionSB: TextStyle = this.captionSB,
        micro: TextStyle = this.micro,
        microSB: TextStyle = this.microSB
    ): UnTypography = UnTypography(
        title1 = title1,
        title2 = title2,
        title2R = title2R,
        title3 = title3,
        headline1 = headline1,
        headline2 = headline2,
        body = body,
        bodyItalic = bodyItalic,
        subhead1 = subhead1,
        subhead2 = subhead2,
        caption = caption,
        captionSB = captionSB,
        micro = micro,
        microSB = microSB
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnTypography) return false

        if (title1 != other.title1) return false
        if (title2 != other.title2) return false
        if (title2R != other.title2R) return false
        if (title3 != other.title3) return false
        if (headline1 != other.headline1) return false
        if (headline2 != other.headline2) return false
        if (body != other.body) return false
        if (bodyItalic != other.bodyItalic) return false
        if (subhead1 != other.subhead1) return false
        if (subhead2 != other.subhead2) return false
        if (caption != other.caption) return false
        if (captionSB != other.captionSB) return false
        if (micro != other.micro) return false
        if (microSB != other.microSB) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title1.hashCode()
        result = 31 * result + title2.hashCode()
        result = 31 * result + title2R.hashCode()
        result = 31 * result + title3.hashCode()
        result = 31 * result + headline1.hashCode()
        result = 31 * result + headline2.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + bodyItalic.hashCode()
        result = 31 * result + subhead1.hashCode()
        result = 31 * result + subhead2.hashCode()
        result = 31 * result + caption.hashCode()
        result = 31 * result + captionSB.hashCode()
        result = 31 * result + micro.hashCode()
        result = 31 * result + microSB.hashCode()
        return result
    }
}

private fun TextStyle.withDefaultFontFamily(default: FontFamily): TextStyle {
    return if (fontFamily != null) this else copy(fontFamily = default)
}

internal val LocalUnTypography = staticCompositionLocalOf { UnTypography() }