package io.horizontalsystems.bankwallet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.horizontalsystems.walletkit.modules.send.SendButton
import io.horizontalsystems.walletkit.modules.send.SendResult
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.walletkit.core.HSCaution
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The send button is disabled through [SendResult], which the view models assign on an IO
 * dispatcher after the click handler returns. These cover the window in between, where the button
 * is still live and a second tap would start a second transaction.
 */
@RunWith(AndroidJUnit4::class)
class SendButtonDoubleTapTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sendTitle = "Send"

    @Test
    fun twoTapsBeforeRecompositionSendOnce() {
        var sends = 0

        composeRule.setContent {
            ComposeAppTheme {
                SendButton(
                    modifier = Modifier,
                    sendResult = null,
                    onClickSend = { sends++ },
                    enabled = true
                )
            }
        }

        // Hold the clock so neither tap can be rescued by a recomposition disabling the button —
        // this is the same frame, which is exactly the case the view model state cannot cover.
        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(sendTitle).performClick()
        composeRule.onNodeWithText(sendTitle).performClick()
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()

        assertEquals(1, sends)
    }

    @Test
    fun failedSendCanBeRetried() {
        var sends = 0
        var result by mutableStateOf<SendResult?>(null)

        composeRule.setContent {
            ComposeAppTheme {
                SendButton(
                    modifier = Modifier,
                    sendResult = result,
                    onClickSend = { sends++ },
                    enabled = true
                )
            }
        }

        composeRule.onNodeWithText(sendTitle).performClick()
        composeRule.waitForIdle()

        result = SendResult.Failed(HSCaution(TranslatableString.PlainString("nope")))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(sendTitle).performClick()
        composeRule.waitForIdle()

        assertEquals(2, sends)
    }
}
