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
 * transaction.
 *
 * A v0 message may load extra accounts from on-chain address lookup tables; those addresses are not
 * in the message and cannot be resolved offline. A transfer whose recipient is one of them is
 * reported as [AccountRef.LookupTable] and the display MUST make that visible (an explicit "unknown
 * recipient" row plus the [Warning.HiddenRecipient] banner) — silently omitting the recipient of a
 * transfer whose amount IS shown would let a malicious dApp drain funds behind a normal-looking
 * "Transfer / 12.5 SOL" screen.
 */
object WCSolanaTxSummary {

    private const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
    private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"

    private const val ASSOCIATED_TOKEN_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"

    // Programs whose instructions never move or delegate funds; they accompany almost every dApp
    // transaction (priority fees, memos) and so do not count as undisplayed actions.
    private val BENIGN_PROGRAMS = setOf(
        "ComputeBudget111111111111111111111111111111",
        "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr", // Memo v2
        "Memo1UhkJRfHyvLMcVucJwxXeuD728EqVDDwQDxFMNo", // Memo v1
    )

    // Associated Token Account `Create` (0, or empty data) / `CreateIdempotent` (1): the payer is
    // debited the rent-exempt minimum, but the created account's address is derived from
    // (owner, mint) and cannot be chosen by the dApp, so nothing goes anywhere unexpected.
    // `RecoverNested` (2) and anything else is unknown.
    private val BENIGN_ATA_INSTRUCTIONS = setOf(0, 1)

    // Token instructions used around a transfer (wSOL wrapping) that cannot move a balance
    // anywhere: initialize (1, 18) and syncNative (17). `CloseAccount` (9) is handled explicitly
    // because it sweeps the account's lamports to a destination; Approve (4), SetAuthority (6),
    // Burn (8) etc. are unknown.
    private val BENIGN_TOKEN_INSTRUCTIONS = setOf(1, 17, 18)

    // Swap aggregators — their presence means the transaction is a swap (Jupiter routes some legs
    // through DFlow).
    private val SWAP_PROGRAMS = setOf(
        "JUP6LkbZbjS1jKKwapdHNy74zcZ3tLUZoi5QNyVTaV4", // Jupiter v6
        "DF1ow4tspfHX9JwWJsAb9epbkA8hmpSEAtxXy1V27QBH", // DFlow
        "3i5JeuZuUxeKtVysUnwQNGerJP2bSMX9fTFfS4Nxe3Br", // LI.FI
        "HNarfxC3kYMMhFkxUFeYb8wHVdPzY5t9pupqW5fL2meM", // 1inch Fusion Swap
    )

    internal enum class Method { SWAP, TRANSFER }

    /**
     * Why the user must be warned before approving. Declared most severe FIRST, so natural ordering
     * (`sorted()`, `minOrNull()`) yields the most severe warning of a batch.
     */
    enum class Warning {
        /** A transfer was decoded but its recipient lives in an address lookup table and can't be shown. */
        HiddenRecipient,

        /** Nothing material could be decoded — parse failure, or no known swap and no transfer. */
        Unreadable,

        /** Something was decoded, but the transaction also invokes programs whose effect isn't shown. */
        UnknownInstructions,
    }

    /**
     * The decoded display for a transaction: [sections] are the rows (Method, Value, From, To,
     * Network), and [warning] is non-null when the user would be blind-signing — either nothing
     * material could be decoded, or a transfer's recipient is hidden in a lookup table. Callers
     * must prepend [warningSection] for a non-null warning.
     */
    data class Summary(
        val sections: List<SectionViewItem>,
        val warning: Warning?,
    )

    /**
     * Display rows for [serializedTransaction], in order: Method, Value, From (when the paying
     * account is not [walletAddress]), To (per directly-decodable transfer), Network, plus a
     * [Summary.warning]. Best-effort: on any parse failure the rows are empty and the warning is
     * [Warning.Unreadable]. Wallet and Fee (when available) are added by the caller.
     *
     * [walletAddress] is the connected account's base58 address, when known; it is used to flag
     * transfers that are paid by some other account rather than the wallet.
     */
    fun summary(serializedTransaction: ByteArray, walletAddress: String?): Summary {
        val decoded = try {
            decode(serializedTransaction)
        } catch (e: Throwable) {
            return Summary(sections = emptyList(), warning = Warning.Unreadable)
        }

        val rows = mutableListOf<ViewItem>()
        var hiddenRecipient = false

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

            // The account whose authority moves the funds. When it is a known address other than
            // the connected wallet, say so — the wallet's signature is then not what pays.
            val payer = transfer.payer
            if (payer is AccountRef.Static && walletAddress != null && payer.address != walletAddress) {
                rows.add(ViewItem.Address(Translator.getString(R.string.TransactionInfo_From), payer.address))
            }

            when (val destination = transfer.destination) {
                is AccountRef.Static ->
                    rows.add(ViewItem.Address(Translator.getString(R.string.Send_Confirmation_To), destination.address))

                AccountRef.LookupTable -> {
                    hiddenRecipient = true
                    rows.add(
                        ViewItem.Value(
                            Translator.getString(R.string.Send_Confirmation_To),
                            Translator.getString(R.string.WalletConnect_Solana_UnknownRecipient),
                            ValueType.Warning
                        )
                    )
                }
            }
        }

        rows.add(ViewItem.Value(Translator.getString(R.string.WalletConnect_Solana_Network), "Solana", ValueType.Regular))

        val warning = when {
            hiddenRecipient -> Warning.HiddenRecipient
            // No material action surfaced: no method (not a known swap) and no directly-decodable
            // transfer. The lone Network row alone tells the user nothing about what they authorize.
            decoded.method == null && decoded.transfers.isEmpty() -> Warning.Unreadable
            // A decoded transfer must not vouch for the instructions next to it: a token Approve
            // or an unknown program call alongside a dust transfer would otherwise pass unremarked.
            decoded.hasUnknownInstructions -> Warning.UnknownInstructions
            else -> null
        }

        return Summary(sections = listOf(SectionViewItem(rows)), warning = warning)
    }

    /**
     * A prominent caution banner for [warning] (see [Summary.warning]). Callers prepend it to
     * their rows so the user is warned they are signing/broadcasting without seeing where the
     * funds go.
     */
    fun warningSection(warning: Warning): SectionViewItem = SectionViewItem(
        listOf(
            when (warning) {
                Warning.Unreadable -> ViewItem.Alert(
                    title = Translator.getString(R.string.WalletConnect_Solana_UnreadableTransaction_Title),
                    text = Translator.getString(R.string.WalletConnect_Solana_UnreadableTransaction),
                    critical = false
                )

                // Funds provably leave an account, to an address the user cannot see: red.
                Warning.HiddenRecipient -> ViewItem.Alert(
                    title = Translator.getString(R.string.WalletConnect_Solana_HiddenRecipient_Title),
                    text = Translator.getString(R.string.WalletConnect_Solana_HiddenRecipient),
                    critical = true
                )

                Warning.UnknownInstructions -> ViewItem.Alert(
                    title = Translator.getString(R.string.WalletConnect_Solana_UnknownInstructions_Title),
                    text = Translator.getString(R.string.WalletConnect_Solana_UnknownInstructions),
                    critical = false
                )
            }
        )
    )

    /** An account referenced by an instruction. */
    internal sealed class AccountRef {
        /** A key present in the message's static account list. */
        data class Static(val address: String) : AccountRef()

        /** A key loaded from an on-chain address lookup table; not resolvable offline. */
        object LookupTable : AccountRef()
    }

    internal data class Decoded(
        val method: Method?,
        val transfers: List<Transfer>,
        /** True for a v0 message that loads accounts from at least one lookup table. */
        val usesLookupTables: Boolean,
        /** True when an instruction invokes a program, or a program instruction, this decoder does not display. */
        val hasUnknownInstructions: Boolean,
    )

    internal data class Transfer(
        val isSol: Boolean,
        val amount: BigInteger,
        val decimals: Int?,
        /** SOL: the `from` account. SPL: the token-account `owner` (the signing authority). */
        val payer: AccountRef,
        val destination: AccountRef,
    )

    private class RawInstruction(val programId: String?, val accountIndices: IntArray, val data: ByteArray)

    // Manual wire-format walk (compact-u16 "shortvec" lengths, 32-byte account keys). Mirrors the
    // kit's RawTransactionParser, extended to capture each top-level instruction's accounts + data
    // and the v0 address-table-lookup section, so an account index can be classified as static,
    // lookup-table-loaded, or out of range (malformed).
    internal fun decode(bytes: ByteArray): Decoded {
        var offset = 0

        fun readByte(): Int = bytes[offset++].toInt() and 0xFF

        fun readLength(): Int {
            var length = 0
            var shift = 0
            while (true) {
                val byte = readByte()
                length = length or ((byte and 0x7F) shl shift)
                if (byte and 0x80 == 0) break
                shift += 7
                // A compact-u16 is at most 3 bytes (u16 max 65535); a longer run is malformed.
                require(shift < 21) { "shortvec length malformed" }
            }
            // A length can never exceed the bytes left to read (each element consumes >= 1 byte), so
            // reject an over-large count BEFORE it drives a List/IntArray/ByteArray allocation
            // (untrusted input must not be able to request a huge allocation and OOM the wallet).
            require(length in 0..bytes.size) { "shortvec length out of range" }
            return length
        }

        val signatureCount = readLength()
        offset += signatureCount * 64

        // Versioned messages are marked by the high bit of the first message byte, with the version
        // in the low seven bits; legacy has no prefix. Only v0 exists today and only its layout is
        // known here, so any other version is rejected rather than parsed as if it were v0.
        val versioned = offset < bytes.size && bytes[offset].toInt() and 0x80 != 0
        if (versioned) {
            val version = readByte() and 0x7F
            require(version == 0) { "unsupported message version $version" }
        }

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
        val instructions = List(instructionCount) {
            // Program ids are always static keys (the runtime forbids loading programs from lookup
            // tables); an unknown one just makes the instruction unrecognized.
            val programId = accountKeys.getOrNull(readByte())
            val instructionAccountCount = readLength()
            val accountIndices = IntArray(instructionAccountCount) { readByte() }
            val dataLength = readLength()
            val data = bytes.copyOfRange(offset, offset + dataLength)
            offset += dataLength
            RawInstruction(programId, accountIndices, data)
        }

        // v0 only: the address table lookups that follow the instructions. Each is a 32-byte table
        // key plus shortvec lists of writable and readonly indexes into that table. The loaded
        // accounts are appended after the static keys (all writable first, then all readonly), so
        // only their COUNT matters for classifying an account index.
        var loadedAccountCount = 0
        if (versioned) {
            repeat(readLength()) {
                offset += 32
                val writable = readLength()
                offset += writable
                val readonly = readLength()
                offset += readonly
                loadedAccountCount += writable + readonly
            }
        }
        require(offset <= bytes.size) { "transaction truncated" }

        fun resolve(index: Int): AccountRef = when {
            index < accountKeys.size -> AccountRef.Static(accountKeys[index])
            index < accountKeys.size + loadedAccountCount -> AccountRef.LookupTable
            else -> throw IllegalArgumentException("account index out of range")
        }

        val transfers = mutableListOf<Transfer>()
        var isSwap = false
        var hasUnknownInstructions = false

        instructions.forEach { instruction ->
            // An index that names no static key cannot be a program (programs are never loaded
            // from lookup tables) — the instruction is undisplayable rather than ignorable.
            val programId = instruction.programId
            if (programId == null) {
                hasUnknownInstructions = true
                return@forEach
            }
            if (programId in SWAP_PROGRAMS) isSwap = true

            val data = instruction.data

            // An instruction that names fewer accounts than its layout needs is malformed — refuse
            // to guess rather than render a partial transfer.
            fun accountAt(position: Int): AccountRef {
                val index = instruction.accountIndices.getOrNull(position)
                    ?: throw IllegalArgumentException("instruction is missing account $position")
                return resolve(index)
            }

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
                // System instructions carry a u32 LE discriminator.
                SYSTEM_PROGRAM -> {
                    val discriminator = if (data.size >= 4 && data[1].toInt() == 0 && data[2].toInt() == 0 && data[3].toInt() == 0) {
                        data[0].toInt() and 0xFF
                    } else {
                        -1
                    }
                    when {
                        // `transfer` (2): u64 lamports; accounts [from, to].
                        discriminator == 2 && data.size >= 12 ->
                            transfers.add(
                                Transfer(isSol = true, amount = u64LE(4), decimals = null, payer = accountAt(0), destination = accountAt(1))
                            )

                        // `createAccount` (0): u64 lamports, u64 space, owner; accounts [funder, new].
                        // The funder is debited `lamports` into the new account, whose key the dApp
                        // may control (it co-signs), so show it as the SOL transfer it is.
                        discriminator == 0 && data.size >= 52 ->
                            transfers.add(
                                Transfer(isSol = true, amount = u64LE(4), decimals = null, payer = accountAt(0), destination = accountAt(1))
                            )

                        // createAccountWithSeed (3), assign, nonce ops... are not displayed.
                        else -> hasUnknownInstructions = true
                    }
                }

                ASSOCIATED_TOKEN_PROGRAM ->
                    if ((data.firstOrNull()?.toInt()?.and(0xFF) ?: 0) !in BENIGN_ATA_INSTRUCTIONS) {
                        hasUnknownInstructions = true
                    }

                // SPL Token instructions carry a u8 discriminator.
                TOKEN_PROGRAM, TOKEN_2022_PROGRAM ->
                    when (val discriminator = data.firstOrNull()?.toInt()?.and(0xFF)) {
                        // `Transfer` (3): accounts [source, dest, owner]
                        3 -> if (data.size >= 9) {
                            transfers.add(Transfer(false, u64LE(1), null, payer = accountAt(2), destination = accountAt(1)))
                        } else {
                            hasUnknownInstructions = true
                        }
                        // `TransferChecked` (12): accounts [source, mint, dest, owner]
                        12 -> if (data.size >= 10) {
                            transfers.add(Transfer(false, u64LE(1), data[9].toInt() and 0xFF, payer = accountAt(3), destination = accountAt(2)))
                        } else {
                            hasUnknownInstructions = true
                        }

                        // `CloseAccount` (9): accounts [account, destination, owner]. Sweeps every
                        // lamport in the token account (a wSOL balance included) to `destination`,
                        // so it is only harmless when that is the owner itself (unwrapping SOL).
                        9 -> {
                            val destination = accountAt(1)
                            val owner = accountAt(2)
                            if (destination !is AccountRef.Static || destination != owner) hasUnknownInstructions = true
                        }

                        in BENIGN_TOKEN_INSTRUCTIONS -> Unit
                        else -> hasUnknownInstructions = true
                    }

                in SWAP_PROGRAMS, in BENIGN_PROGRAMS -> Unit
                else -> hasUnknownInstructions = true
            }
        }

        val method = when {
            isSwap -> Method.SWAP
            transfers.isNotEmpty() -> Method.TRANSFER
            else -> null
        }

        return Decoded(
            method,
            transfers,
            usesLookupTables = loadedAccountCount > 0,
            hasUnknownInstructions = hasUnknownInstructions,
        )
    }
}
