package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.managers.SolanaKitWrapper
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.entities.nft.NftUid
import io.horizontalsystems.walletkit.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaSwapTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaTransactionRecord
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.Transaction
import io.horizontalsystems.walletkit.entities.transactionrecords.solana.SolanaTransactionInfo
import io.horizontalsystems.solanakit.transactions.KnownPrograms
import java.math.BigDecimal

class SolanaTransactionConverter(
        private val coinManager: ICoinManager,
        private val source: TransactionSource,
        private val baseToken: Token,
        solanaKitWrapper: SolanaKitWrapper
) {
    private val userAddress = solanaKitWrapper.solanaKit.receiveAddress

    // The display label of the first recognized swap program this transaction invoked, or null.
    private fun swapExchangeName(transaction: Transaction): String? =
        transaction.programIds?.split(" ")?.firstNotNullOfOrNull { swapProgramLabels[it] }

    // The swap-relevant leg of one side: the SPL transfer when a native-SOL leg rides along
    // (token-account rent), otherwise the single/first leg (a genuinely-SOL swap side).
    private fun primaryTransfer(transfers: List<SolanaTransactionRecord.Transfer>): SolanaTransactionRecord.Transfer? =
        transfers.firstOrNull { (it.value as? TransactionValue.CoinValue)?.token != baseToken }
            ?: transfers.firstOrNull()

    // When a side is exactly one token (non-SOL) leg plus one or more *small* SOL legs (associated-
    // token-account rent that rides along an SPL transfer), the SOL legs are a network cost, not
    // separate transfers — reduce the side to just the token leg. A SOL leg that is a genuine
    // payment is NOT rent, so it is kept: collapsing it would silently hide a real SOL transfer that
    // shares a transaction with a token transfer. Any other shape is returned unchanged (a lone SOL
    // transfer, multiple token legs, etc.).
    //
    // Whether the SOL is rent is decided STRUCTURALLY from [createdTokenAccount] (SolanaKit reports
    // whether the transaction created a token account):
    //   true  -> an account was created, so the SOL is rent -> collapse (still bounded by
    //            [maxSolRent] so a real payment bundled with a create isn't hidden);
    //   false -> no account was created, so the SOL leg is a real transfer -> keep it;
    //   null  -> unknown (a row synced before this was tracked) -> fall back to the amount bound.
    //
    // Caveat: `createdTokenAccount` is transaction-level and the SOL side is a single NET balance
    // change, so we can't attribute exactly how many lamports were rent. [maxSolRent] is therefore
    // kept to ~one account's rent, which bounds any hidden bundled payment to sub-cent dust; a SOL
    // side larger than that (a real payment, or several created accounts) is kept as a composite
    // transfer — showing more, never hiding. Exact per-leg attribution would need SolanaKit support.
    private fun collapseTokenWithSolRent(
        transfers: List<SolanaTransactionRecord.Transfer>,
        createdTokenAccount: Boolean?
    ): List<SolanaTransactionRecord.Transfer> {
        if (transfers.size <= 1) return transfers

        val (solLegs, tokenLegs) = transfers.partition { (it.value as? TransactionValue.CoinValue)?.token == baseToken }
        if (tokenLegs.size != 1 || solLegs.isEmpty()) return transfers

        val solLegsWithinRent = solLegs.all { leg ->
            val amount = (leg.value as? TransactionValue.CoinValue)?.value?.abs()
            amount != null && amount <= maxSolRent
        }
        if (!solLegsWithinRent) return transfers

        val solIsRent = when (createdTokenAccount) {
            true -> true
            false -> false
            null -> true
        }

        return if (solIsRent) tokenLegs else transfers
    }

    suspend fun transactionRecord(fullTransaction: FullTransaction): SolanaTransactionRecord {
        val transaction = fullTransaction.transaction
        val incomingTransfers = mutableListOf<SolanaTransactionRecord.Transfer>()
        val outgoingTransfers = mutableListOf<SolanaTransactionRecord.Transfer>()

        transaction.amount?.let {
            if (transaction.from == userAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, it.multiply(BigDecimal.valueOf(-1)).movePointLeft(baseToken.decimals))
                outgoingTransfers.add(SolanaTransactionRecord.Transfer(transaction.to, transactionValue))
            } else if (transaction.to == userAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, it.movePointLeft(baseToken.decimals))
                incomingTransfers.add(SolanaTransactionRecord.Transfer(transaction.from, transactionValue))
            } else {}
        }

        for (fullTokenTransfer in fullTransaction.tokenTransfers) {
            val tokenTransfer = fullTokenTransfer.tokenTransfer
            val mintAccount = fullTokenTransfer.mintAccount
            val query = TokenQuery(BlockchainType.Solana, TokenType.Spl(tokenTransfer.mintAddress))
            val token = coinManager.getToken(query)

            // SolanaKit stores transfer amounts unsigned (direction lives in `incoming`), but the
            // transactions list derives the displayed +/- from the value's sign — negate outgoing
            // amounts, matching the SOL leg above. NFT values stay unsigned: NFT rendering passes
            // direction explicitly and shows the count.
            val amount = if (tokenTransfer.incoming) tokenTransfer.amount else tokenTransfer.amount.negate()

            val transactionValue = when {
                token != null -> TransactionValue.CoinValue(token, amount.movePointLeft(token.decimals))
                mintAccount.isNft -> TransactionValue.NftValue(
                    NftUid.Solana(mintAccount.address),
                    tokenTransfer.amount,
                    mintAccount.name,
                    mintAccount.symbol
                )
                else -> TransactionValue.RawValue(value = amount.toBigInteger())
            }

            if (tokenTransfer.incoming) {
                incomingTransfers.add(SolanaTransactionRecord.Transfer(transaction.from, transactionValue))
            } else {
                outgoingTransfers.add(SolanaTransactionRecord.Transfer(transaction.to, transactionValue))
            }
        }

        // A recognized DEX interaction (via SolanaKit KnownPrograms) renders as a swap — we key purely
        // on the invoked program. Same-chain swaps (Jupiter) carry legs on both sides; a CROSS-CHAIN
        // LI.FI swap FROM Solana carries only the OUTGOING side (the bought asset lands on another
        // chain); a pending swap carries no legs yet (the kit stores no balance changes until
        // confirmation) — all are swaps. A side can carry a spurious SOL leg next to the real SPL one
        // (tx fee / token-account rent), so each side prefers its non-SOL leg via `primaryTransfer`
        // (`valueIn`/`valueOut` are null when that side has no leg).
        val exchangeName = swapExchangeName(transaction)
        if (exchangeName != null) {
            return SolanaSwapTransactionRecord(
                transaction = transaction.essentials(),
                baseToken = baseToken,
                source = source,
                exchangeName = exchangeName,
                valueIn = primaryTransfer(outgoingTransfers)?.value,
                valueOut = primaryTransfer(incomingTransfers)?.value
            )
        }

        // A plain SPL send/receive also moves a little SOL for associated-token-account rent (paid
        // when the recipient's token account has to be created), so the transaction carries a SOL
        // leg alongside the token leg. That is one logical transfer (the token) plus a network cost
        // — not two transfers — so collapse a "single token leg + accompanying SOL rent/fee" side to
        // just the token leg. Without this, such sends fall through to "Unknown Transaction".
        val effectiveIncoming = collapseTokenWithSolRent(incomingTransfers, transaction.createdTokenAccount)
        val effectiveOutgoing = collapseTokenWithSolRent(outgoingTransfers, transaction.createdTokenAccount)

        return when {
            (effectiveIncoming.size == 1 && effectiveOutgoing.isEmpty()) -> {
                val transfer = effectiveIncoming.first()
                val spam = App.spamManager.isSpam(
                    transaction.hash.toByteArray(),
                    listOf(TransferEvent(transfer.address, transfer.value)),
                    source,
                    transaction.timestamp,
                    null
                )
                SolanaIncomingTransactionRecord(transaction.essentials(), baseToken, source, transfer.address, transfer.value, spam)
            }

            (effectiveIncoming.isEmpty() && effectiveOutgoing.size == 1) -> {
                val transfer = effectiveOutgoing.first()
                SolanaOutgoingTransactionRecord(transaction.essentials(), baseToken, source, transfer.address, transfer.value, transfer.address == userAddress)
            }

            else -> {
                val incomingEvents = incomingTransfers.map { TransferEvent(it.address, it.value) }
                val spam = if (incomingEvents.isNotEmpty()) {
                    val outgoingEvents = outgoingTransfers.map { TransferEvent(it.address, it.value) }
                    App.spamManager.isSpam(
                        transaction.hash.toByteArray(),
                        incomingEvents + outgoingEvents,
                        source,
                        transaction.timestamp,
                        null
                    )
                } else {
                    false
                }
                SolanaUnknownTransactionRecord(transaction.essentials(), baseToken, source, incomingTransfers, outgoingTransfers, spam)
            }
        }
    }

    companion object {
        // Upper bound for a SOL leg that may be treated as associated-token-account rent rather than
        // a real payment. Set to ~one account's rent: the rent-exempt minimum for a classic SPL
        // associated token account (165 bytes) is ~0.00204 SOL, but a Token-2022 account also
        // carries extension data — immutable-owner plus the mint-required account extensions
        // (transfer-fee amount, transfer-hook flag, …) — observed on mainnet at 0.00207–0.00220 SOL
        // (e.g. 0.00210888 for a Pump.fun ATA). The bound must sit ABOVE that range: at 0.0021 a
        // Token-2022 send that created the recipient's account kept its rent leg and fell through
        // to "Unknown Transaction". Kept tight beyond that headroom on purpose — since
        // `createdTokenAccount` is transaction-level and the SOL side is a single NET amount we
        // can't split into rent vs. payment, a real payment bundled with a create pushes the net
        // past this bound and is shown as a composite transfer; only sub-cent dust could ever fold
        // into the rent. A larger SOL leg (a genuine payment, or several created accounts) is
        // likewise kept — showing more, never hiding. See collapseTokenWithSolRent; exact per-leg
        // attribution would need SolanaKit support.
        private val maxSolRent = BigDecimal("0.0023")

        // Display labels for the swap programs SolanaKit recognizes (`Transaction.programIds`).
        // Mirrors the EVM flow, where the exchange contract address maps to a label ("1inch v5").
        private val swapProgramLabels = mapOf(
            KnownPrograms.jupiterV6 to "Jupiter",
            // A distinct aggregator (Jupiter routes some legs through it, but it is also used
            // directly), so label it by its actual program rather than assuming Jupiter.
            KnownPrograms.dflow to "DFlow",
            KnownPrograms.lifi to "LI.FI",
        )
    }

}

private fun Transaction.essentials() = SolanaTransactionInfo(
    hash = hash,
    pending = pending,
    timestamp = timestamp,
    failed = error != null,
    fee = fee,
)
