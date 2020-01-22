package io.horizontalsystems.bankwallet.core.utils

import io.horizontalsystems.bankwallet.core.IEmojiHelper

class EmojiHelper : IEmojiHelper {

    override val multiAlerts = "\uD83D\uDCC8\uD83D\uDCC9"
    private val rocket = "\uD83D\uDE80"
    private val moon = "\uD83C\uDF19"
    private val brokenHeart = "\uD83D\uDC94"
    private val positive5 = "\uD83D\uDE0E"
    private val positive3 = "\uD83D\uDE09"
    private val positive2 = "\uD83D\uDE42"
    private val negative5 = "\uD83D\uDE29"
    private val negative3 = "\uD83D\uDE27"
    private val negative2 = "\uD83D\uDE14"

    override fun title(signedState: Int): String {
        var emoji = if (signedState > 0) rocket else brokenHeart
        if (signedState >= 5) {
            emoji += moon
        }
        return emoji
    }

    override fun body(signedState: Int): String {
        return when (signedState) {
            5 -> positive5
            3 -> positive3
            2 -> positive2
            -5 -> negative5
            -3 -> negative3
            -2 -> negative2
            else -> ""
        }
    }
}
