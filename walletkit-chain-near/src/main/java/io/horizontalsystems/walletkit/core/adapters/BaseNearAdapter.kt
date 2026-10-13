package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.nearkit.NearKit
import io.horizontalsystems.nearkit.models.NearAmount
import io.horizontalsystems.nearkit.crypto.AccountId
import io.horizontalsystems.nearkit.transaction.FeeCalculator
import io.horizontalsystems.nearkit.transaction.Action
import io.horizontalsystems.walletkit.core.IAdapter
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.IReceiveAdapter
import io.horizontalsystems.walletkit.core.ISendNearAdapter
import io.horizontalsystems.walletkit.core.managers.NearKitWrapper
import java.math.BigDecimal
import java.math.BigInteger

/** Shared kit access for the NEAR and NEP-141 token adapters of one account. */
abstract class BaseNearAdapter(
    val kitWrapper: NearKitWrapper,
) : IAdapter, IBalanceAdapter, IReceiveAdapter, ISendNearAdapter {
    val kit: NearKit = kitWrapper.kit

    override val receiveAddress: String = kit.accountId
    override val isMainNet: Boolean = kit.isMainNet
    override val debugInfo: String get() = ""

    val availableNear: BigDecimal get() = NearAmount.toNear(kit.availableBalance)

    override val pendingTransactionHashes: Set<String>
        get() = kit.getPendingTransactions().mapTo(mutableSetOf()) { it.hash }

    override fun validate(address: String) {
        NearKit.validateAccountId(address)
    }

    companion object {
        /**
         * Balance a NEAR transfer needs in the worst case: a NEAR-implicit receiver, which pays
         * for creating the account and its key whether or not it exists, with gas priced at the
         * minimum purchase price. Holds while the block gas price stays below that minimum.
         */
        val WORST_CASE_TRANSFER_REQUIRED: BigDecimal = NearAmount.toNear(
            FeeCalculator.Config.DEFAULT.minGasPurchasePrice.let { price ->
                FeeCalculator().estimate(
                    listOf(Action.Transfer(BigInteger.ONE)),
                    receiver = AccountId.Type.NearImplicit,
                    createsAccount = false,
                    gasPrice = price,
                ).requiredBalance
            }
        )

        /** A transfer to a named account at the usual 10^8 yocto/gas price; shown until an estimate arrives. */
        val DEFAULT_FEE: BigDecimal = BigDecimal("0.0000446")
    }
}
