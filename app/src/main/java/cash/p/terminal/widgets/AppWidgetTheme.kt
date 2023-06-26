package cash.p.terminal.widgets

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
import cash.p.terminal.ui.compose.Black50
import cash.p.terminal.ui.compose.Dark
import cash.p.terminal.ui.compose.Green50
import cash.p.terminal.ui.compose.GreenD
import cash.p.terminal.ui.compose.GreenL
import cash.p.terminal.ui.compose.Grey
import cash.p.terminal.ui.compose.Grey50
import cash.p.terminal.ui.compose.Light
import cash.p.terminal.ui.compose.LightGrey
import cash.p.terminal.ui.compose.Red20
import cash.p.terminal.ui.compose.Red50
import cash.p.terminal.ui.compose.RedD
import cash.p.terminal.ui.compose.RedL
import cash.p.terminal.ui.compose.Steel10
import cash.p.terminal.ui.compose.Steel20
import cash.p.terminal.ui.compose.SteelDark
import cash.p.terminal.ui.compose.SteelLight
import cash.p.terminal.ui.compose.Yellow20
import cash.p.terminal.ui.compose.Yellow50
import cash.p.terminal.ui.compose.YellowD
import cash.p.terminal.ui.compose.YellowL
import cash.p.terminal.ui.compose.darkPalette
import cash.p.terminal.ui.compose.lightPalette

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
        bran = ColorProvider(lightPalette.bran, darkPalette.bran),
        leah = ColorProvider(lightPalette.leah, darkPalette.leah),
        claude = ColorProvider(lightPalette.claude, darkPalette.claude),
        lawrence = ColorProvider(lightPalette.lawrence, darkPalette.lawrence),
        jeremy = ColorProvider(lightPalette.jeremy, darkPalette.jeremy),
        laguna = ColorProvider(lightPalette.laguna, darkPalette.laguna),
        raina = ColorProvider(lightPalette.raina, darkPalette.raina),
    )
}

data class ColorProviders(
    val jacob: ColorProvider,
    val remus: ColorProvider,
    val lucian: ColorProvider,
    val tyler: ColorProvider,
    val bran: ColorProvider,
    val leah: ColorProvider,
    val claude: ColorProvider,
    val lawrence: ColorProvider,
    val jeremy: ColorProvider,
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
val steelLight = SteelLight
val steelDark = SteelDark
val steel10 = Steel10
val steel20 = Steel20
val grey = Grey
val grey50 = Grey50
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

