package io.horizontalsystems.walletkit.uiv3.components.controls

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R

/**
 * The filled check that marks a selected control or a finished step, with
 * its circle [size] across.
 *
 * The icon keeps 2 dp of padding around the circle inside its 24 dp frame.
 * The ring or placeholder drawn at [size] for the other states has none, so
 * the icon is drawn past [size] for its circle to be as large as they are.
 * It still takes up [size] in the layout.
 */
@Composable
fun FilledCheckMark(
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color = ComposeAppTheme.colors.orange,
) {
    Icon(
        modifier = modifier
            .size(size)
            .requiredSize(size * (ICON_FRAME / ICON_CIRCLE)),
        painter = painterResource(R.drawable.done_e_filled_24),
        contentDescription = null,
        tint = tint,
    )
}

private const val ICON_FRAME = 24f
private const val ICON_CIRCLE = 20f
