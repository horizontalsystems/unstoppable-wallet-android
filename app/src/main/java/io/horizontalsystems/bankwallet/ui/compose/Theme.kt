package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density


val lightPalette = Colors(
    jacob = YellowL,
    remus = GreenL,
    lucian = RedL,
    tyler = Bright,
    leah = Dark,
    lawrence = White,
    laguna = LagunaL,
    raina = White50,
    andy = Steel,
    blade = Light,
)

val darkPalette = Colors(
    jacob = YellowD,
    remus = GreenD,
    lucian = RedD,
    tyler = Black,
    leah = Bright,
    lawrence = Dark,
    laguna = LagunaD,
    raina = Color(0x1a6E7899),
    andy = Smoke,
    blade = Carbon,
)

@Composable
fun ComposeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {

    val colors = if (darkTheme) {
        darkPalette
    } else {
        lightPalette
    }

    //custom styles
    ProvideLocalAssets(colors = colors, typography = Typography()) {
        //material styles
        MaterialTheme(
            content = content
        )
    }

}

private val MyRippleAlpha = RippleAlpha(0.5f, 0.5f, 0.5f, 0.5f)

@OptIn(ExperimentalMaterialApi::class)
val MyRippleConfiguration =
    RippleConfiguration(color = Color.Black, rippleAlpha = MyRippleAlpha)

object ComposeAppTheme {
    val colors: Colors
        @Composable
        get() = LocalColors.current

    val typography: Typography
        @Composable
        get() = LocalTypography.current
}

@Composable
fun ProvideLocalAssets(
    colors: Colors,
    typography: Typography,
    content: @Composable () -> Unit
) {

    val colorPalette = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }
    colorPalette.update(colors)
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalColors provides colorPalette,
        LocalTypography provides typography,
        LocalDensity provides Density(currentDensity.density, fontScale = 1f),
        content = content
    )
}

val LocalColors = compositionLocalOf<Colors> {
    error("No Colors provided")
}
