package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.welcome.GLOW_SETTLED
import io.horizontalsystems.walletkit.modules.welcome.WelcomeGlow
import io.horizontalsystems.walletkit.uiv3.TestTags

/**
 * The frame the passcode screens share: the glow, a bar with only the back
 * arrow, [topBlock] in the space that is left, then [numpad] and under it
 * the room for [bottomButton]. That room is kept when there is no button,
 * so the keypad sits at the same height on every passcode screen.
 *
 * @param onBack null leaves the bar empty, for a screen that cannot be left.
 */
@Composable
fun PinScreen(
    onBack: (() -> Unit)?,
    topBlock: @Composable ColumnScope.() -> Unit,
    numpad: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomButton: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        WelcomeGlow(animate = false, startAt = GLOW_SETTLED)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // A short screen keeps only the room the button needs under the keypad
            val bottomRoom = if (maxHeight < SHORT_SCREEN_BELOW) 72.dp else 104.dp

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    onBack?.let { BackButton(it) }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    content = topBlock,
                )

                numpad()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomRoom)
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    bottomButton()
                }
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .testTag(TestTags.PIN_BACK)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = ComposeAppTheme.colors.leah),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.arrow_m_left_24),
            contentDescription = stringResource(R.string.Button_Back),
            tint = ComposeAppTheme.colors.grey,
        )
    }
}

private val SHORT_SCREEN_BELOW = 700.dp
