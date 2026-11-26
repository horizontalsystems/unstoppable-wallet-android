package io.horizontalsystems.walletkit.modules.usersubscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.welcome.GLOW_SETTLED
import io.horizontalsystems.walletkit.modules.welcome.WelcomeGlow
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.uiv3.components.bottombars.ButtonsGroupVertical

/**
 * The frame the Premium pages share: the glow, a close button in place of a
 * top bar, the star, then [content] under it and [button] at the bottom.
 * The content scrolls above the button when the screen is too short for it.
 */
@Composable
fun PremiumPage(
    onClose: () -> Unit,
    closeTestTag: String,
    button: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeAppTheme.colors.tyler)
    ) {
        WelcomeGlow(animate = false, startAt = GLOW_SETTLED)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .testTag(closeTestTag)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, color = ComposeAppTheme.colors.leah),
                                onClick = onClose
                            ),
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.Button_Close),
                        tint = ComposeAppTheme.colors.grey,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(54.dp),
                        painter = painterResource(R.drawable.polistar_24),
                        contentDescription = null,
                        tint = ComposeAppTheme.colors.orange,
                    )
                }
                VSpacer(16.dp)
                content()
            }

            ButtonsGroupVertical {
                button()
            }
        }
    }
}

@Composable
fun highlightText(
    text: String,
    textColor: Color,
    highlightPart: String,
    highlightColor: Color
): AnnotatedString {

    return buildAnnotatedString {
        withStyle(SpanStyle(color = textColor)) {
            val highlightIndex = text
                .lowercase()
                .indexOf(highlightPart.lowercase())

            if (highlightIndex != -1) {
                append(text.substring(0, highlightIndex))

                withStyle(
                    SpanStyle(color = highlightColor)
                ) {
                    append(
                        text.substring(
                            highlightIndex,
                            highlightIndex + highlightPart.length
                        )
                    )
                }

                append(
                    text.substring(highlightIndex + highlightPart.length)
                )
            } else {
                append(text)
            }
        }
    }
}
