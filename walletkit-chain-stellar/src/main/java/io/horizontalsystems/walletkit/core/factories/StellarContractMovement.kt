package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.StellarAsset
import java.math.BigDecimal

// The account's net asset movements of one contract call, aggregated by asset across all
// of Horizon's balance changes: repeated movements of one asset are summed, and an asset
// appearing on both sides nets out. Signed amounts: sold negative, received positive.
internal sealed class StellarContractMovement {
    data class Movement(val asset: StellarAsset, val amount: BigDecimal)

    data class Swap(val sold: Movement, val received: Movement) : StellarContractMovement()
    data class Outgoing(val movement: Movement, val counterparty: String?) : StellarContractMovement()
    data class Incoming(val movement: Movement, val counterparty: String?) : StellarContractMovement()

    // No movement touches the account.
    data object None : StellarContractMovement()

    // More than one asset moved in one direction — the single-pair record types cannot
    // state it truthfully, so the operation stays a generic contract call rather than
    // showing understated amounts.
    data object Unrepresentable : StellarContractMovement()

    companion object {
        fun resolve(changes: List<Operation.ContractBalanceChange>, accountId: String): StellarContractMovement {
            val netByAsset = mutableMapOf<StellarAsset, BigDecimal>()
            changes.forEach { change ->
                if (change.from == accountId) {
                    netByAsset.merge(change.asset, change.amount.negate(), BigDecimal::add)
                }
                if (change.to == accountId) {
                    netByAsset.merge(change.asset, change.amount, BigDecimal::add)
                }
            }

            val sold = netByAsset.filterValues { it.signum() < 0 }
            val received = netByAsset.filterValues { it.signum() > 0 }

            fun movement(entry: Map.Entry<StellarAsset, BigDecimal>) = Movement(entry.key, entry.value)

            // The counterparty of the (single) moved asset — where it went or came from.
            fun counterparty(asset: StellarAsset, outgoing: Boolean): String? = changes.firstOrNull {
                it.asset == asset && (if (outgoing) it.from == accountId else it.to == accountId)
            }?.let { if (outgoing) it.to else it.from }

            return when {
                sold.isEmpty() && received.isEmpty() -> None

                sold.size == 1 && received.size == 1 -> Swap(
                    sold = movement(sold.entries.first()),
                    received = movement(received.entries.first()),
                )

                sold.size == 1 && received.isEmpty() -> {
                    val entry = sold.entries.first()
                    Outgoing(movement(entry), counterparty(entry.key, outgoing = true))
                }

                received.size == 1 && sold.isEmpty() -> {
                    val entry = received.entries.first()
                    Incoming(movement(entry), counterparty(entry.key, outgoing = false))
                }

                else -> Unrepresentable
            }
        }
    }
}
