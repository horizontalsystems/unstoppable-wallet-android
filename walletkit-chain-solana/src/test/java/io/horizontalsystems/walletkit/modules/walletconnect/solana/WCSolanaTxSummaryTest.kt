package io.horizontalsystems.walletkit.modules.walletconnect.solana

import io.horizontalsystems.bitcoincore.crypto.Base58
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.util.Base64

class WCSolanaTxSummaryTest {

    // The transaction from the "hidden recipient" bug report: a SystemProgram.transfer of 12.5 SOL
    // whose recipient (account index 2) is loaded from an address lookup table and therefore never
    // appears in the static account keys. As pasted in the report, the message lacks the 0x80
    // version prefix a real v0 wire message carries (the PoC serialized the bare message), so it
    // is kept in both shapes: verbatim, and with the prefix inserted — which is exactly what
    // @solana/web3.js emits for `compileToV0Message([lookupTable])`.
    private val reportedTx = Base64.getDecoder().decode(
        "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAAEC5QOHqTYcPWWb4/o1gWxbcsFv51nXGjb1xO4iD2b6HFsAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQECAAIMAgAAAADdDukCAAAAAQJ3pq+XM5t6yI0YkskERvUAAjCSZvYuU8EYJEmCAAAAAQAA"
    )
    private val hiddenRecipientTx = reportedTx.copyOfRange(0, 65) + byteArrayOf(0x80.toByte()) + reportedTx.copyOfRange(65, reportedTx.size)

    private val victim = "GQyV7SEJdaxZaAZKj3DorTzR2Zg9yiu2WnVduzi7Ks6a"

    private val systemProgram = ByteArray(32)

    private companion object {
        const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
        const val COMPUTE_BUDGET = "ComputeBudget111111111111111111111111111111"
        const val ATA_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
    }
    private val fromKey = ByteArray(32) { 0x11 }
    private val toKey = ByteArray(32) { 0x22 }

    @Test
    fun v0TransferToLookupTableAccount_reportsHiddenRecipient() {
        val decoded = WCSolanaTxSummary.decode(hiddenRecipientTx)

        assertEquals(WCSolanaTxSummary.Method.TRANSFER, decoded.method)
        assertTrue(decoded.usesLookupTables)
        assertEquals(1, decoded.transfers.size)

        val transfer = decoded.transfers.single()
        assertTrue(transfer.isSol)
        assertEquals(BigInteger.valueOf(12_500_000_000L), transfer.amount)
        assertEquals(WCSolanaTxSummary.AccountRef.Static(victim), transfer.payer)
        assertEquals(WCSolanaTxSummary.AccountRef.LookupTable, transfer.destination)
    }

    @Test
    fun reportedBytesWithoutVersionPrefix_areMalformedNotSilentlyPartial() {
        // Without the v0 prefix the message is legacy, has no lookup section, and index 2 points
        // past its two static keys: the old parser rendered "12.5 SOL" with no recipient row; now
        // it must fail decoding so the caller shows the blind-signing warning.
        assertThrows(IllegalArgumentException::class.java) { WCSolanaTxSummary.decode(reportedTx) }
    }

    @Test
    fun legacyTransfer_resolvesStaticRecipient() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, toKey, systemProgram),
            instructions = listOf(solTransfer(programIndex = 2, from = 0, to = 1, lamports = 1_000_000_000L)),
        )

        val decoded = WCSolanaTxSummary.decode(bytes)

        assertFalse(decoded.usesLookupTables)
        val transfer = decoded.transfers.single()
        assertEquals(BigInteger.valueOf(1_000_000_000L), transfer.amount)
        assertEquals(WCSolanaTxSummary.AccountRef.Static(Base58.encode(fromKey)), transfer.payer)
        assertEquals(WCSolanaTxSummary.AccountRef.Static(Base58.encode(toKey)), transfer.destination)
    }

    @Test
    fun v0TransferWithStaticRecipient_resolvesEvenWhenLookupTablesArePresent() {
        val bytes = transaction(
            versioned = true,
            keys = listOf(fromKey, toKey, systemProgram),
            instructions = listOf(solTransfer(programIndex = 2, from = 0, to = 1, lamports = 5L)),
            lookups = listOf(Lookup(writable = 2, readonly = 3)),
        )

        val decoded = WCSolanaTxSummary.decode(bytes)

        assertTrue(decoded.usesLookupTables)
        assertEquals(WCSolanaTxSummary.AccountRef.Static(Base58.encode(toKey)), decoded.transfers.single().destination)
    }

    @Test
    fun accountIndexBeyondStaticAndLoadedKeys_isMalformed() {
        val bytes = transaction(
            versioned = true,
            keys = listOf(fromKey, systemProgram),
            // index 3: static keys are 0..1, the single loaded key is 2 — 3 is out of range.
            instructions = listOf(solTransfer(programIndex = 1, from = 0, to = 3, lamports = 5L)),
            lookups = listOf(Lookup(writable = 1, readonly = 0)),
        )

        assertThrows(IllegalArgumentException::class.java) { WCSolanaTxSummary.decode(bytes) }
    }

    @Test
    fun legacyIndexIntoNonexistentLookupRange_isMalformed() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, systemProgram),
            instructions = listOf(solTransfer(programIndex = 1, from = 0, to = 2, lamports = 5L)),
        )

        assertThrows(IllegalArgumentException::class.java) { WCSolanaTxSummary.decode(bytes) }
    }

    @Test
    fun unknownProgramOnly_hasNoMethodAndNoTransfers() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, ByteArray(32) { 0x33 }),
            instructions = listOf(Instruction(programIndex = 1, accounts = intArrayOf(0), data = byteArrayOf(9, 9, 9))),
        )

        val decoded = WCSolanaTxSummary.decode(bytes)

        assertNull(decoded.method)
        assertTrue(decoded.transfers.isEmpty())
        assertTrue(decoded.hasUnknownInstructions)
    }

    @Test
    fun transferNextToUnknownProgram_isFlaggedAsPartiallyDisplayed() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, toKey, systemProgram, ByteArray(32) { 0x33 }),
            instructions = listOf(
                solTransfer(programIndex = 2, from = 0, to = 1, lamports = 1L),
                Instruction(programIndex = 3, accounts = intArrayOf(0), data = byteArrayOf(9, 9, 9)),
            ),
        )

        val decoded = WCSolanaTxSummary.decode(bytes)

        assertEquals(1, decoded.transfers.size)
        assertTrue(decoded.hasUnknownInstructions)
    }

    @Test
    fun tokenApproveNextToTransfer_isFlagged() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, toKey, systemProgram, Base58.decode(TOKEN_PROGRAM)),
            instructions = listOf(
                solTransfer(programIndex = 2, from = 0, to = 1, lamports = 1L),
                // SPL Token Approve (4): [source, delegate, owner], u64 amount
                Instruction(programIndex = 3, accounts = intArrayOf(0, 1, 0), data = ByteArray(9).also { it[0] = 4 }),
            ),
        )

        assertTrue(WCSolanaTxSummary.decode(bytes).hasUnknownInstructions)
    }

    @Test
    fun computeBudgetAndAtaCreation_areNotFlagged() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, toKey, systemProgram, Base58.decode(COMPUTE_BUDGET), Base58.decode(ATA_PROGRAM)),
            instructions = listOf(
                Instruction(programIndex = 3, accounts = intArrayOf(), data = byteArrayOf(2, 0, 0, 0, 0)), // setComputeUnitLimit
                Instruction(programIndex = 4, accounts = intArrayOf(0, 1, 0), data = byteArrayOf(1)),     // createIdempotent
                solTransfer(programIndex = 2, from = 0, to = 1, lamports = 1L),
            ),
        )

        val decoded = WCSolanaTxSummary.decode(bytes)

        assertEquals(1, decoded.transfers.size)
        assertFalse(decoded.hasUnknownInstructions)
    }

    @Test
    fun unsupportedMessageVersion_isRejected() {
        val bytes = transaction(
            versioned = false,
            keys = listOf(fromKey, toKey, systemProgram),
            instructions = listOf(solTransfer(programIndex = 2, from = 0, to = 1, lamports = 1L)),
        )
        // Insert a version prefix of 1 (0x81) where a v0 message would carry 0x80.
        val v1 = bytes.copyOfRange(0, 65) + byteArrayOf(0x81.toByte()) + bytes.copyOfRange(65, bytes.size)

        assertThrows(IllegalArgumentException::class.java) { WCSolanaTxSummary.decode(v1) }
    }

    @Test
    fun warningsSortMostSevereFirst() {
        // A batch shows its warnings in this order, so a hidden recipient in one transaction is
        // never masked by an unreadable sibling.
        val mixed = listOf(
            WCSolanaTxSummary.Warning.UnknownInstructions,
            WCSolanaTxSummary.Warning.Unreadable,
            WCSolanaTxSummary.Warning.HiddenRecipient,
        )

        assertEquals(
            listOf(
                WCSolanaTxSummary.Warning.HiddenRecipient,
                WCSolanaTxSummary.Warning.Unreadable,
                WCSolanaTxSummary.Warning.UnknownInstructions,
            ),
            mixed.sorted()
        )
        assertEquals(WCSolanaTxSummary.Warning.HiddenRecipient, mixed.minOrNull())
    }

    // --- minimal wire-format builder -------------------------------------------------------------

    private class Instruction(val programIndex: Int, val accounts: IntArray, val data: ByteArray)
    private class Lookup(val writable: Int, val readonly: Int)

    private fun solTransfer(programIndex: Int, from: Int, to: Int, lamports: Long): Instruction {
        val data = ByteArray(12)
        data[0] = 2
        for (i in 0 until 8) data[4 + i] = (lamports ushr (8 * i)).toByte()
        return Instruction(programIndex, intArrayOf(from, to), data)
    }

    private fun transaction(
        versioned: Boolean,
        keys: List<ByteArray>,
        instructions: List<Instruction>,
        lookups: List<Lookup> = emptyList(),
    ): ByteArray {
        val out = mutableListOf<Byte>()
        fun byte(v: Int) = out.add(v.toByte())
        fun bytes(b: ByteArray) = b.forEach { out.add(it) }

        byte(1) // one signature slot
        bytes(ByteArray(64))
        if (versioned) byte(0x80)
        byte(1); byte(0); byte(1) // header
        byte(keys.size)
        keys.forEach { bytes(it) }
        bytes(ByteArray(32)) // blockhash
        byte(instructions.size)
        instructions.forEach { instruction ->
            byte(instruction.programIndex)
            byte(instruction.accounts.size)
            instruction.accounts.forEach { byte(it) }
            byte(instruction.data.size)
            bytes(instruction.data)
        }
        if (versioned) {
            byte(lookups.size)
            lookups.forEach { lookup ->
                bytes(ByteArray(32) { 0x44 })
                byte(lookup.writable)
                repeat(lookup.writable) { byte(it) }
                byte(lookup.readonly)
                repeat(lookup.readonly) { byte(it) }
            }
        }
        return out.toByteArray()
    }
}
