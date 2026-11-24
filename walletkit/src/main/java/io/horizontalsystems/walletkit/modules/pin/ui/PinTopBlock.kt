package io.horizontalsystems.walletkit.modules.pin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.horizontalsystems.designkit.theme.ComposeAppTheme
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.pin.PinModule
import io.horizontalsystems.walletkit.modules.pin.unlock.PinUnlockModule.InputState
import io.horizontalsystems.walletkit.ui.compose.animations.shake
import io.horizontalsystems.walletkit.uiv3.TestTags
import kotlin.math.roundToInt

/**
 * What a passcode screen shows above the keypad: the lock, the title, the
 * entered digits as dots and one message under them.
 *
 * The message is the first that applies: the error, the time the passcode
 * is disabled until, the attempts left, then [description], which gives
 * way as soon as a digit is entered.
 */
@Composable
fun PinTopBlock(
    title: String,
    enteredCount: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    error: String? = null,
    showShakeAnimation: Boolean = false,
    inputState: InputState = InputState.Enabled(),
    onShakeAnimationFinish: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        PinTopBlockContent(
            title = title,
            enteredCount = enteredCount,
            // Too short for the design's block and a message under it:
            // the lock gives up the space around it
            compact = maxHeight < COMPACT_BELOW,
            description = description,
            error = error,
            showShakeAnimation = showShakeAnimation,
            inputState = inputState,
            onShakeAnimationFinish = onShakeAnimationFinish,
        )
    }
}

/**
 * Places the lock, the title with the dots, and the message under them.
 * The free height is split as in the design, where 45 is left above the
 * lock and 98 under the dots, and the message is drawn in the lower part,
 * so it never moves what is above it. A message taller than that part takes
 * the difference from the space above the lock, and when the text still
 * does not fit, on a short screen with a wrapped title and message, the
 * lock is left out.
 */
@Composable
private fun PinTopBlockContent(
    title: String,
    enteredCount: Int,
    compact: Boolean,
    description: String?,
    error: String?,
    showShakeAnimation: Boolean,
    inputState: InputState,
    onShakeAnimationFinish: (() -> Unit)?,
) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            PinLock(compact)
            PinHeading(title, enteredCount, showShakeAnimation, onShakeAnimationFinish)
            Box(
                modifier = Modifier
                    .widthIn(max = CONTENT_WIDTH)
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                PinMessage(
                    description = description.takeIf { enteredCount == 0 },
                    error = error,
                    inputState = inputState,
                )
            }
        },
    ) { measurables, constraints ->
        // Unbounded height, to learn what each part needs
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val lock = measurables[0].measure(loose)
        val heading = measurables[1].measure(loose)
        val message = measurables[2].measure(loose)

        val height = constraints.maxHeight
        val lockHeight = if (lock.height + heading.height + message.height <= height) lock.height else 0
        val free = (height - lockHeight - heading.height).coerceAtLeast(0)
        val designShare = (free * SPACE_BELOW / (SPACE_ABOVE + SPACE_BELOW)).roundToInt()
        val below = maxOf(designShare, message.height).coerceAtMost(free)
        val above = free - below

        layout(constraints.maxWidth, height) {
            fun center(width: Int) = (constraints.maxWidth - width) / 2

            if (lockHeight > 0) lock.placeRelative(center(lock.width), above)
            heading.placeRelative(center(heading.width), above + lockHeight)
            message.placeRelative(center(message.width), above + lockHeight + heading.height)
        }
    }
}

@Composable
private fun PinLock(compact: Boolean) {
    Box(
        modifier = Modifier.size(if (compact) 48.dp else 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(R.drawable.lock_filled_24),
            tint = ComposeAppTheme.colors.grey,
            contentDescription = null,
        )
    }
}

@Composable
private fun PinHeading(
    title: String,
    enteredCount: Int,
    showShakeAnimation: Boolean,
    onShakeAnimationFinish: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            modifier = Modifier
                .widthIn(max = CONTENT_WIDTH)
                .fillMaxWidth()
                .testTag(TestTags.PIN_TITLE),
            text = title,
            style = ComposeAppTheme.typography.title3,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.Center,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(top = 24.dp)
                .shake(
                    enabled = showShakeAnimation,
                    onAnimationFinish = { onShakeAnimationFinish?.invoke() }
                )
                .padding(16.dp)
        ) {
            repeat(PinModule.PIN_COUNT) {
                IndicatorCircle(it < enteredCount)
            }
        }
    }
}

/** The one message under the dots: the first of these that applies. */
@Composable
private fun PinMessage(description: String?, error: String?, inputState: InputState) {
    val colors = ComposeAppTheme.colors
    when {
        error != null -> {
            Message(error, colors.lucian, TestTags.PIN_ERROR)
        }

        inputState is InputState.Locked -> {
            Message(
                text = stringResource(R.string.Unlock_WalletDisabledUntil, inputState.until),
                color = colors.lucian,
                testTag = TestTags.PIN_DISABLED_UNTIL,
            )
        }

        inputState is InputState.Enabled && inputState.attemptsLeft != null -> {
            Message(
                text = stringResource(R.string.Unlock_AttemptsLeft, inputState.attemptsLeft),
                color = colors.lucian,
                testTag = TestTags.PIN_ATTEMPTS_LEFT,
            )
        }

        description != null -> {
            Message(description, colors.grey, TestTags.PIN_DESCRIPTION)
        }
    }
}

@Composable
private fun Message(text: String, color: Color, testTag: String) {
    Text(
        modifier = Modifier.testTag(testTag),
        text = text,
        style = ComposeAppTheme.typography.subhead,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun IndicatorCircle(active: Boolean) {
    val color = if (active) ComposeAppTheme.colors.orange else ComposeAppTheme.colors.blade
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private const val SPACE_ABOVE = 45f
private const val SPACE_BELOW = 98f
private val CONTENT_WIDTH = 264.dp
private val COMPACT_BELOW = 260.dp

@Preview
@Composable
private fun Preview_PinTopBlockEnabled() {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinTopBlock(
                title = "Create passcode",
                description = "Passcode will be used to unlock",
                enteredCount = 0,
            )
        }
    }
}

@Preview
@Composable
private fun Preview_PinTopBlockLocked() {
    ComposeAppTheme {
        Column(
            modifier = Modifier
                .background(color = ComposeAppTheme.colors.tyler)
        ) {
            PinTopBlock(
                title = "Enter passcode",
                enteredCount = 0,
                inputState = InputState.Locked("12:33")
            )
        }
    }
}
