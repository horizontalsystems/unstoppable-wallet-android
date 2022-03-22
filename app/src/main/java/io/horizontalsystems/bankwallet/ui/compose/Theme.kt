package io.horizontalsystems.bankwallet.ui.compose

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.LocalImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder


val lightPalette = Colors(
    jacob = YellowL,
    remus = GreenL,
    lucian = RedL,
    oz = Dark,
    tyler = Light,
    bran = Dark,
    leah = SteelDark,
    claude = Color.White,
    lawrence = Color.White,
    jeremy = SteelLight
)

val darkPalette = Colors(
    jacob = YellowD,
    remus = GreenD,
    lucian = RedD,
    oz = Light,
    tyler = Dark,
    bran = LightGrey,
    leah = SteelLight,
    claude = Dark,
    lawrence = SteelDark,
    jeremy = Steel20
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
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .componentRegistry {
            add(SvgDecoder(LocalContext.current))
            if (SDK_INT >= 28) {
                add(ImageDecoderDecoder(LocalContext.current))
            } else {
                add(GifDecoder())
            }
        }
        .build()

    val colorPalette = remember {
        // Explicitly creating a new object here so we don't mutate the initial [colors]
        // provided, and overwrite the values set in it.
        colors.copy()
    }
    colorPalette.update(colors)
    CompositionLocalProvider(
        LocalColors provides colorPalette,
        LocalTypography provides typography,
        LocalImageLoader provides imageLoader,
        content = content
    )
}

val LocalColors = compositionLocalOf<Colors> {
    error("No Colors provided")
}
