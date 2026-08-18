package io.horizontalsystems.walletkit.modules.walletconnect.solana

import io.horizontalsystems.bitcoincore.crypto.Base58
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Best-effort decoder + display builder for a serialized Solana transaction (legacy or v0) shown on
 * the WalletConnect sign screen. Solana transactions are opaque instruction bundles, so — unlike an
 * EVM transaction — there is no single to/value/method. This surfaces a coarse method (Swap when a
 * swap aggregator is invoked, otherwise Transfer), the network, and any directly-decodable System
 * (SOL) / SPL-token transfers found in the TOP-LEVEL instructions.
 *
 * Swap amounts are intentionally NOT shown: for a Jupiter swap they happen inside CPI/inner
 * instructions and via address lookup tables, neither of which is visible by parsing the outer
 * transaction. Accounts loaded from a v0 lookup table cannot be resolved to an address either, so a
 * transfer that references one is reported without a destination.
 */
object WCSolanaTxSummary {

    private const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
    private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"

    // Swap aggregators — their presence means the transaction is a swap (Jupiter routes some legs
    // through DFlow).
    private val SWAP_PROGRAMS = setOf(
        "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4", // Jupiter v6
        "DF1ow4tspfHX9JwWJsAb9epbkA8hmpSEAtxXy1V27QBH", // DFlow
        "3i5JeuZuUxeKtVysUnwQNGerJP2bSMX9fTFfS4Nxe3Br", // LI.FI
    )

    private enum class Method { SWAP, TRANSFER }

    /**
     * Display rows for [serializedTransaction], in order: Method, Value, To (per directly-decodable
     * transfer), Network. Best-effort: returns an empty list on any parse failure, so the caller can
     * still show its generic header. Wallet and Fee (when available) are added by the caller.
     *
     * [peerName] is accepted for parity with the request but not rendered (the dApp row was dropped
     * from this layout).
     */
    fun sections(serializedTransaction: ByteArray, peerName: String?): List<SectionViewItem> {
        val decoded = try {
            decode(serializedTransaction)
        } catch (e: Throwable) {
            return emptyList()
        }

        val rows = mutableListOf<ViewItem>()

        decoded.method?.let { method ->
            val value = when (method) {
                Method.SWAP -> R.string.WalletConnect_Solana_MethodSwap
                Method.TRANSFER -> R.string.WalletConnect_Solana_MethodTransfer
            }
            rows.add(
                ViewItem.Value(Translator.getString(R.string.WalletConnect_Solana_Method), Translator.getString(value), ValueType.Regular)
            )
        }

        decoded.transfers.forEach { transfer ->
            val amount = if (transfer.isSol) {
                "${BigDecimal(transfer.amount).movePointLeft(9).stripTrailingZeros().toPlainString()} SOL"
            } else if (transfer.decimals != null) {
                BigDecimal(transfer.amount).movePointLeft(transfer.decimals).stripTrailingZeros().toPlainString()
            } else {
                transfer.amount.toString()
            }
            rows.add(ViewItem.Value(Translator.getString(R.string.WalletConnect_Solana_Value), amount, ValueType.Outgoing))

            transfer.destination?.let {
                rows.add(ViewItem.Address(Translator.getString(R.string.Send_Confirmation_To), it))
            }
        }

        rows.add(ViewItem.Value(Translator.getString(R.string.WalletConnect_Solana_Network), "Solana", ValueType.Regular))

        return listOf(SectionViewItem(rows))
    }

    private data class Decoded(
        val method: Method?,
        val transfers: List<Transfer>,
    )

    private data class Transfer(
        val isSol: Boolean,
        val amount: BigInteger,
        val decimals: Int?,
        val destination: String?,
    )

    // Manual wire-format walk (compact-u16 "shortvec" lengths, 32-byte account keys). Mirrors the
    // kit's RawTransactionParser, extended to capture each top-level instruction's accounts + data.
    private fun decode(bytes: ByteArray): Decoded {
        var offset = 0

        fun readByte(): Int = bytes[offset++].toInt() and 0xFF

        fun readLength(): Int {
            var length = 0
            var shift = 0
            while (true) {
                val byte = readByte()
                length = length or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) return length
                shift += 7
            }
        }

        val signatureCount = readLength()
        offset += signatureCount * 64

        // V0 messages are marked by the high bit of the first message byte; legacy has none.
        if (offset < bytes.size && bytes[offset].toInt() and 0x80 != 0) offset++

        readByte() // numRequiredSignatures
        offset += 2 // numReadonlySignedAccounts, numReadonlyUnsignedAccounts

        val accountCount = readLength()
        val accountKeys = List(accountCount) {
            val key = Base58.encode(bytes.copyOfRange(offset, offset + 32))
            offset += 32
            key
        }

        offset += 32 // recent blockhash

        val instructionCount = readLength()
        val transfers = mutableListOf<Transfer>()
        var isSwap = false

        repeat(instructionCount) {
            val programId = accountKeys.getOrNull(readByte())

            val instructionAccountCount = readLength()
            val accountIndices = IntArray(instructionAccountCount) { readByte() }

            val dataLength = readLength()
            val data = bytes.copyOfRange(offset, offset + dataLength)
            offset += dataLength

            if (programId == null) return@repeat
            if (programId in SWAP_PROGRAMS) isSwap = true

            fun keyAt(instructionIndex: Int): String? =
                accountIndices.getOrNull(instructionIndex)?.let { accountKeys.getOrNull(it) }

            // u64 is unsigned; build a BigInteger so amounts with the high bit set (>= 2^63) stay
            // positive instead of overflowing a signed Long and rendering as a negative amount.
            fun u64LE(from: Int): BigInteger {
                var value = BigInteger.ZERO
                for (i in 0 until 8) {
                    value = value.or(BigInteger.valueOf(data[from + i].toLong() and 0xFF).shiftLeft(i * 8))
                }
                return value
            }

            when (programId) {
                // System `transfer`: u32 discriminator 2, then u64 lamports; accounts [from, to].
                SYSTEM_PROGRAM ->
                    if (data.size >= 12 && data[0].toInt() == 2 && data[1].toInt() == 0 &&
                        data[2].toInt() == 0 && data[3].toInt() == 0
                    ) {
                        transfers.add(Transfer(isSol = true, amount = u64LE(4), decimals = null, destination = keyAt(1)))
                    }

                // SPL Token `Transfer` (3) / `TransferChecked` (12).
                TOKEN_PROGRAM, TOKEN_2022_PROGRAM ->
                    when (data.firstOrNull()?.toInt()?.and(0xFF)) {
                        3 -> if (data.size >= 9)
                            transfers.add(Transfer(false, u64LE(1), null, keyAt(1))) // [source, dest, owner]
                        12 -> if (data.size >= 10)
                            transfers.add(Transfer(false, u64LE(1), data[9].toInt() and 0xFF, keyAt(2))) // [source, mint, dest, owner]
                    }
            }
        }

        val method = when {
            isSwap -> Method.SWAP
            transfers.isNotEmpty() -> Method.TRANSFER
            else -> null
        }

        return Decoded(method, transfers)
    }
}
