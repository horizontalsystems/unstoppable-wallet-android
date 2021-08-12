package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

val lightPalette = UwColors(
    jacob = YellowL,
    remus = GreenL,
    lucian = RedL,
    oz = OzL,
    tyler = TylerL,
    bran = BranL,
    leah = LeahL,
    claude = ClaudeL,
)

val darkPalette = UwColors(
    jacob = YellowD,
    remus = GreenD,
    lucian = RedD,
    oz = OzD,
    tyler = TylerD,
    bran = BranD,
    leah = LeahD,
    claude = ClaudeD,
)

@Composable
fun ComposeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val unColors = if (darkTheme) {
        darkPalette
    } else {
        lightPalette
    }

    //custom styles
    ProvideLocalAssets(colors = unColors, typography = UnTypography()) {
        //material styles
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }

}

object UnstoppableComponentsAppTheme {
    val colors: UwColors
        @Composable
        get() = LocalUnColors.current

    val typography: UnTypography
        @Composable
        get() = LocalUnTypography.current
}

@Composable
fun ProvideLocalAssets(
    colors: UwColors,
    typography: UnTypography,
    content: @Composable () -> Unit
) {
    val colorPalette = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }
    colorPalette.update(colors)
    CompositionLocalProvider(
        LocalUnColors provides colorPalette,
        LocalUnTypography provides typography,
        content = content
    )
}

val LocalUnColors = compositionLocalOf<UwColors> {
    error("No UnColors provided")
}
