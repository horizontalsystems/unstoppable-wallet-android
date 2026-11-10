package io.horizontalsystems.walletkit.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.horizontalsystems.designkit.theme.Black50
import io.horizontalsystems.designkit.theme.Dark
import io.horizontalsystems.designkit.theme.Green50
import io.horizontalsystems.designkit.theme.GreenD
import io.horizontalsystems.designkit.theme.GreenL
import io.horizontalsystems.designkit.theme.Grey
import io.horizontalsystems.designkit.theme.Light
import io.horizontalsystems.designkit.theme.LightGrey
import io.horizontalsystems.designkit.theme.Orange
import io.horizontalsystems.designkit.theme.Red20
import io.horizontalsystems.designkit.theme.Red50
import io.horizontalsystems.designkit.theme.RedD
import io.horizontalsystems.designkit.theme.RedL
import io.horizontalsystems.designkit.theme.darkPalette
import io.horizontalsystems.designkit.theme.lightPalette

object AppWidgetTheme {
    val colors: ColorProviders
        @Composable
        @ReadOnlyComposable
        get() = LocalColorProviders.current

    val textStyles: TextStyles = TextStyles()
}

class TextStyles {
    @Composable
    fun c3(textAlign: TextAlign = TextAlign.Start) =
        TextStyle(
            color = AppWidgetTheme.colors.orange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = textAlign
        )

    @Composable
    fun d1(textAlign: TextAlign = TextAlign.Start) =
        TextStyle(
            color = AppWidgetTheme.colors.grey,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign
        )

    @Composable
    fun d3(textAlign: TextAlign = TextAlign.Start) =
        TextStyle(
            color = AppWidgetTheme.colors.orange,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign
        )

    @Composable
    fun micro(textAlign: TextAlign = TextAlign.Start) =
        TextStyle(
            color = AppWidgetTheme.colors.grey,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            textAlign = textAlign
        )
}

@Composable
fun AppWidgetTheme(colors: ColorProviders = AppWidgetTheme.colors, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColorProviders provides colors) {
        content()
    }
}

internal val LocalColorProviders = staticCompositionLocalOf {
    ColorProviders(
        jacob = ColorProvider(lightPalette.jacob, darkPalette.jacob),
        remus = ColorProvider(lightPalette.remus, darkPalette.remus),
        lucian = ColorProvider(lightPalette.lucian, darkPalette.lucian),
        tyler = ColorProvider(lightPalette.tyler, darkPalette.tyler),
        leah = ColorProvider(lightPalette.leah, darkPalette.leah),
        lawrence = ColorProvider(lightPalette.lawrence, darkPalette.lawrence),
        laguna = ColorProvider(lightPalette.laguna, darkPalette.laguna),
        raina = ColorProvider(lightPalette.raina, darkPalette.raina),
        orange = ColorProvider(Orange),
    )
}

data class ColorProviders(
    val jacob: ColorProvider,
    val remus: ColorProvider,
    val lucian: ColorProvider,
    val tyler: ColorProvider,
    val leah: ColorProvider,
    val lawrence: ColorProvider,
    val laguna: ColorProvider,
    val raina: ColorProvider,
    val orange: ColorProvider,

    //base colors
    val grey: ColorProvider = ColorProvider(Grey)
)

//base colors
val transparent = Color.Transparent
val dark = Dark
val light = Light
val white = Color.White
val black50 = Black50
val issykBlue = Color(0xFF3372FF)
val lightGrey = LightGrey
val grey = Grey

val greenD = GreenD
val greenL = GreenL
val green50 = Green50
val redD = RedD
val redL = RedL
val elenaD = Color(0xFF6E7899)
val red50 = Red50
val red20 = Red20

