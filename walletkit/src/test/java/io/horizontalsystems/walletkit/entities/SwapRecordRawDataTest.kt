package io.horizontalsystems.walletkit.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * The raw-data text is the only way to get a stored swap record off a release build, so it
 * must carry every column, including the ones the Swap Info page does not show, and keep
 * carrying new columns as they are added.
 */
class SwapRecordRawDataTest {

    private val record = SwapRecord(
        id = 7,
        accountId = "account-1",
        timestamp = 1_700_000_000_000L,
        providerId = "u_near",
        providerName = "NEAR Intents",
        tokenInUid = "bitcoin",
        tokenInCoinCode = "BTC",
        tokenInCoinUid = "bitcoin",
        tokenInBadge = null,
        tokenInBlockchainTypeUid = "bitcoin",
        tokenOutUid = "solana",
        tokenOutCoinCode = "SOL",
        tokenOutCoinUid = "solana",
        tokenOutBadge = null,
        tokenOutBlockchainTypeUid = "solana",
        amountIn = "0.01",
        amountOut = "1.5",
        amountOutMin = null,
        recipientAddress = "SoLRecipient111",
        customRecipientAddress = false,
        sourceAddress = "bc1source",
        transactionHash = null,
        providerSwapId = null,
        fromAsset = null,
        toAsset = null,
        depositAddress = null,
        status = "Completed",
    )

    @Test
    fun everyColumnOfTheRecordIsListed() {
        val columns = SwapRecord::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
            .map { it.name }
            .toSet()
        val listed = record.toRawData().lines().map { it.substringBefore(": ") }.toSet()

        assertEquals(columns, listed - "checksum")
    }

    @Test
    fun hiddenRecipientColumnsAndEmptyColumnsAreWritten() {
        val lines = record.toRawData().lines()

        assertEquals("id: 7", lines.first())
        assertTrue("recipientAddress: SoLRecipient111" in lines)
        assertTrue("customRecipientAddress: false" in lines)
        assertTrue("transactionHash: " in lines)
        assertTrue("estimatedTime: " in lines)
    }

    @Test
    fun checksumIsTheSha256PrefixOfTheLinesAboveIt() {
        val text = record.toRawData()
        val body = text.substringBeforeLast("\nchecksum: ")
        val checksum = text.substringAfterLast("\nchecksum: ")

        assertEquals(rawDataChecksum(body), checksum)
        assertEquals(16, checksum.length)
        assertTrue(checksum.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun checksumChangesWhenAColumnChanges() {
        val edited = record.copy(recipientAddress = "SoLRecipient112").toRawData()

        assertTrue(edited.substringAfterLast("checksum: ") != record.toRawData().substringAfterLast("checksum: "))
    }
}
