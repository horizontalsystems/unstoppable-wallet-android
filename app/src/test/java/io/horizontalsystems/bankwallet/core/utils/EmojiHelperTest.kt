package io.horizontalsystems.bankwallet.core.utils

import junit.framework.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EmojiHelperTest : Spek({

    val emojiHelper by memoized { EmojiHelper() }

    val rocket = "\uD83D\uDE80"
    val moon = "\uD83C\uDF19"
    val brokenHeart = "\uD83D\uDC94"
    val multiAlerts = "\uD83D\uDCC8\uD83D\uDCC9"

    val positive5 = "\uD83D\uDE0E"
    val positive3 = "\uD83D\uDE09"
    val positive2 = "\uD83D\uDE42"
    val negative5 = "\uD83D\uDE29"
    val negative3 = "\uD83D\uDE27"
    val negative2 = "\uD83D\uDE14"

    val positiveEmojiBodyArray = listOf("", positive5, positive3, positive2)
    val negativeEmojiBodyArray = listOf(negative5, negative3, negative2)
    val positiveEmojiTitleArray = listOf(brokenHeart, rocket + moon, rocket, rocket)
    val negativeEmojiTitleArray = listOf(brokenHeart, brokenHeart, brokenHeart)
    val positiveStates = listOf(0, 5, 3, 2)
    val negativeStates = listOf(-5, -3, -2)


    describe("#smartSort") {

        describe("body") {
            it("loop all states") {
                positiveStates.forEachIndexed { index, state ->
                    val positiveEmoji = emojiHelper.body(state)
                    assertEquals(positiveEmojiBodyArray[index], positiveEmoji)
                }
                negativeStates.forEachIndexed { index, state ->
                    val negativeEmoji = emojiHelper.body(state)
                    assertEquals(negativeEmojiBodyArray[index], negativeEmoji)
                }
            }
        }

        describe("title") {
            it("loop all states") {
                positiveStates.forEachIndexed { index, state ->
                    val positiveEmoji = emojiHelper.title(state)
                    assertEquals(positiveEmoji, positiveEmojiTitleArray[index])
                }
                negativeStates.forEachIndexed { index, state ->
                    val negativeEmoji = emojiHelper.title(state)
                    assertEquals(negativeEmoji, negativeEmojiTitleArray[index])
                }
            }
        }

        describe("#title") {
            it("check multi alerts emoji") {
                assertEquals(emojiHelper.multiAlerts, multiAlerts)
            }
        }

    }
})
