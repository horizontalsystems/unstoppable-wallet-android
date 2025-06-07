package cash.p.terminal.tangem.common

import com.tangem.crypto.CryptoUtils

object TwinsHelper {

    /**
     * Card compatibility
     * cb61 <-> cb62
     * cb64 <-> cb65
     *
     */
    private const val FIRST_CARD_FIRST_SERIES = "CB61"
    private const val SECOND_CARD_FIRST_SERIES = "CB62"
    private const val FIRST_CARD_SECOND_SERIES = "CB64"
    private const val SECOND_CARD_SECOND_SERIES = "CB65"

    @Suppress("MagicNumber")
    fun verifyTwinPublicKey(issuerData: ByteArray, cardWalletPublicKey: ByteArray?): Boolean {
        if (issuerData.size < 65 || cardWalletPublicKey == null) return false

        val publicKey = issuerData.sliceArray(0 until 65)
        val signedKey = issuerData.sliceArray(65 until issuerData.size)
        return CryptoUtils.verify(cardWalletPublicKey, publicKey, signedKey)
    }

    fun getTwinCardNumber(cardId: String): TwinCardNumber? {
        val isFirstCard = cardId.startsWith(FIRST_CARD_FIRST_SERIES) ||
                cardId.startsWith(FIRST_CARD_SECOND_SERIES)
        if (isFirstCard) return TwinCardNumber.First

        val isSecondCard = cardId.startsWith(SECOND_CARD_FIRST_SERIES) ||
                cardId.startsWith(SECOND_CARD_SECOND_SERIES)
        if (isSecondCard) return TwinCardNumber.Second
        return null
    }

    @Suppress("MagicNumber")
    fun getTwinCardIdForUser(cardId: String): String {
        if (cardId.length < 16) return cardId

        val twinCardId = cardId.substring(11..14)
        val twinCardNumber = getTwinCardNumber(cardId)?.number ?: 1
        return "$twinCardId #$twinCardNumber"
    }

    /**
     * Twins compatibility
     * cb61 <-> cb62
     * cb64 <-> cb65
     */
    fun isTwinsCompatible(firstCardId: String, secondCardId: String): Boolean {
        return secondCardId.startsWith(getCompatibleTwinCardId(firstCardId))
    }

    private fun getCompatibleTwinCardId(cardId: String): String {
        return when {
            cardId.startsWith(FIRST_CARD_FIRST_SERIES) -> SECOND_CARD_FIRST_SERIES
            cardId.startsWith(FIRST_CARD_SECOND_SERIES) -> SECOND_CARD_SECOND_SERIES
            cardId.startsWith(SECOND_CARD_FIRST_SERIES) -> FIRST_CARD_FIRST_SERIES
            cardId.startsWith(SECOND_CARD_SECOND_SERIES) -> FIRST_CARD_SECOND_SERIES
            else -> {
                "unknown"
            }
        }
    }
}

enum class TwinCardNumber(val number: Int) {
    First(1), Second(2);

    fun pairNumber(): TwinCardNumber = when (this) {
        First -> Second
        Second -> First
    }
}
