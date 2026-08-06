package io.horizontalsystems.walletkit.core

import cash.z.ecc.android.sdk.model.Proposal
import io.horizontalsystems.walletkit.core.adapters.zcash.ZcashAdapter
import java.math.BigDecimal

interface ISendZcashAdapter {
    val availableBalance: BigDecimal

    suspend fun validate(address: String): ZcashAdapter.ZCashAddressType
    suspend fun fee(amount: BigDecimal, address: String, memo: String): BigDecimal
    suspend fun proposeTransfer(amount: BigDecimal, address: String, memo: String): Proposal
    suspend fun sendProposal(proposal: Proposal): String?
}
