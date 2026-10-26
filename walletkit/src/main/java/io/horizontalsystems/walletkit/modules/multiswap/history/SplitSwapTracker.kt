package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import java.math.BigDecimal

/**
 * Reads a `/v3/split/track` answer into the record's stage and its outputs' own states.
 *
 * The body is the `/v3/track` shape: one deposit leg for the whole deposit (no output index),
 * then a payout leg per output tagged with `meta.outputIndex`, plus a refund leg where the
 * provider reports one. Legs are matched to outputs by that index, never by address or asset:
 * several outputs may share both. An output with no leg on a poll keeps what it last had.
 */
object SplitSwapTracker {

    data class Result(
        // Null when the server's status is unknown: the record is left as it is.
        val status: SwapStatus?,
        val outputs: List<SwapRecordOutput>,
        // The outputs' total once every payout is observed; null until then.
        val amountOut: BigDecimal?,
        val pauseReason: String?,
    )

    fun map(response: UnstoppableAPI.Response.Track, outputs: List<SwapRecordOutput>): Result {
        val legs = response.legs.orEmpty()
        val depositLeg = legs.firstOrNull { it.outputIndex == null }
        val depositDone = depositLeg?.status == "completed"
        val payoutLegs = legs.filter { it.outputIndex != null && !it.isRefund }.associateBy { it.outputIndex!! }
        val refundLegs = legs.filter { it.outputIndex != null && it.isRefund }.associateBy { it.outputIndex!! }

        val status = aggregateStatus(response.status, depositLeg, payoutLegs.values)

        val updatedOutputs = outputs.map { output ->
            val payout = payoutLegs[output.index]
            val refund = refundLegs[output.index]
            val ownStatus = when {
                refund?.status == "completed" -> SwapStatus.Refunded
                payout != null -> payoutStatus(payout.status, depositDone)
                else -> output.swapStatus
            }
            output.copy(
                status = finalOutputStatus(status, ownStatus).name,
                amountOut = payout?.toAmount?.toBigDecimalOrNull()?.takeIf { it > BigDecimal.ZERO }?.toPlainString() ?: output.amountOut,
                txHash = payout?.hash ?: refund?.hash ?: output.txHash,
            )
        }

        val paidAmounts = payoutLegs.values.mapNotNull { leg ->
            leg.takeIf { it.status == "completed" }?.toAmount?.toBigDecimalOrNull()
        }
        val amountOut = if (outputs.isNotEmpty() && paidAmounts.size == outputs.size) {
            paidAmounts.fold(BigDecimal.ZERO) { sum, amount -> sum + amount }.takeIf { it > BigDecimal.ZERO }
        } else {
            null
        }

        return Result(
            status = status,
            outputs = updatedOutputs,
            amountOut = amountOut,
            pauseReason = response.meta?.pauseReason?.takeIf { status == SwapStatus.ActionRequired },
        )
    }

    // The split's stage from the least-finished output, as the server aggregates it. While it
    // is in flight the stage follows the legs: the deposit until it confirms, then the swap,
    // then the payouts once any is on its way.
    private fun aggregateStatus(
        status: String,
        depositLeg: UnstoppableAPI.Response.Track.Leg?,
        payoutLegs: Collection<UnstoppableAPI.Response.Track.Leg>,
    ): SwapStatus? = when (status) {
        "not_started", "signed" -> SwapStatus.Depositing
        "pending", "swapping" -> when {
            depositLeg != null && depositLeg.status != "completed" -> SwapStatus.Depositing
            payoutLegs.any { it.status == "pending" } -> SwapStatus.Sending
            else -> SwapStatus.Swapping
        }

        "completed" -> SwapStatus.Completed
        "refunded" -> SwapStatus.Refunded
        "failed" -> SwapStatus.Failed
        "expired" -> SwapStatus.Expired
        "action_required" -> SwapStatus.ActionRequired
        else -> null
    }

    // A payout leg's status is that payout's own: not started until the swap sends it, pending
    // while it confirms, completed once paid.
    private fun payoutStatus(legStatus: String, depositDone: Boolean): SwapStatus = when (legStatus) {
        "completed" -> SwapStatus.Completed
        "pending" -> SwapStatus.Sending
        "not_started", "signed" -> if (depositDone) SwapStatus.Swapping else SwapStatus.Depositing
        "refunded" -> SwapStatus.Refunded
        "failed" -> SwapStatus.Failed
        else -> SwapStatus.Swapping
    }

    // An output that the provider has not paid takes the split's terminal or held state; one
    // already paid or refunded keeps it, since the provider cannot take it back.
    private fun finalOutputStatus(aggregate: SwapStatus?, own: SwapStatus): SwapStatus {
        if (own == SwapStatus.Completed || own == SwapStatus.Refunded) return own
        return when (aggregate) {
            SwapStatus.Completed -> SwapStatus.Completed
            SwapStatus.Refunded -> SwapStatus.Refunded
            SwapStatus.Failed -> SwapStatus.Failed
            SwapStatus.Expired -> SwapStatus.Expired
            SwapStatus.ActionRequired -> SwapStatus.ActionRequired
            SwapStatus.Depositing -> SwapStatus.Depositing
            else -> own
        }
    }
}
