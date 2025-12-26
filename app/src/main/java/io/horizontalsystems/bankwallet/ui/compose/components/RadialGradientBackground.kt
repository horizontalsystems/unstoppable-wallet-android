package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun RadialBackground() {
    var size by remember { mutableStateOf(Size.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                size = coordinates.size.toSize()
            }
            .background(ComposeAppTheme.colors.tyler)
    ) {
        // 1st Radial Gradient - Yellow
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x80EDD716),
                            Color(0x00EDD716)
                        ),
                        radius = 250.dp.dpToPx(),
                        center = Offset(
                            x = -50.dp.dpToPx(), // Half outside left side
                            y = 300.dp.dpToPx()
                        )
                    )
                )
                .fillMaxSize()
        )

        // 2nd Radial Gradient - Orange
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x40FF9B26),
                            Color(0x00FF9B26)
                        ),
                        radius = 250.dp.dpToPx(),
                        center = Offset(
                            x = size.width / 2f, // Center of the screen
                            y = 400.dp.dpToPx()
                        )
                    )
                )
                .fillMaxSize()
        )

        // 3rd Radial Gradient - Blue
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x73003C74),
                            Color(0x00003C74)
                        ),
                        radius = 250.dp.dpToPx(),
                        center = Offset(
                            x = size.width + 50.dp.dpToPx(), // Half outside right side
                            y = 500.dp.dpToPx()
                        )
                    )
                )
                .fillMaxSize()
        )
    }
}

@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }

@Preview
@Composable
fun RadialBackgroundPreview() {
    ComposeAppTheme {
        RadialBackground()
    }
}