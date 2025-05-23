package io.horizontalsystems.bankwallet.widgets

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
import io.horizontalsystems.bankwallet.ui.compose.Black50
import io.horizontalsystems.bankwallet.ui.compose.Dark
import io.horizontalsystems.bankwallet.ui.compose.Green50
import io.horizontalsystems.bankwallet.ui.compose.GreenD
import io.horizontalsystems.bankwallet.ui.compose.GreenL
import io.horizontalsystems.bankwallet.ui.compose.Grey
import io.horizontalsystems.bankwallet.ui.compose.Light
import io.horizontalsystems.bankwallet.ui.compose.LightGrey
import io.horizontalsystems.bankwallet.ui.compose.Red20
import io.horizontalsystems.bankwallet.ui.compose.Red50
import io.horizontalsystems.bankwallet.ui.compose.RedD
import io.horizontalsystems.bankwallet.ui.compose.RedL
import io.horizontalsystems.bankwallet.ui.compose.Yellow20
import io.horizontalsystems.bankwallet.ui.compose.Yellow50
import io.horizontalsystems.bankwallet.ui.compose.YellowD
import io.horizontalsystems.bankwallet.ui.compose.YellowL
import io.horizontalsystems.bankwallet.ui.compose.darkPalette
import io.horizontalsystems.bankwallet.ui.compose.lightPalette

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
            color = AppWidgetTheme.colors.jacob,
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
            color = AppWidgetTheme.colors.jacob,
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
val yellow50 = Yellow50
val yellow20 = Yellow20

val yellowD = YellowD
val yellowL = YellowL
val greenD = GreenD
val greenL = GreenL
val green50 = Green50
val redD = RedD
val redL = RedL
val elenaD = Color(0xFF6E7899)
val red50 = Red50
val red20 = Red20

