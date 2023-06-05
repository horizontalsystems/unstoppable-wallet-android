package io.horizontalsystems.bankwallet.modules.transactions

import io.horizontalsystems.tronkit.models.AssetIssueContract
import io.horizontalsystems.tronkit.models.Contract
import io.horizontalsystems.tronkit.models.CreateSmartContract
import io.horizontalsystems.tronkit.models.FreezeBalanceV2Contract
import io.horizontalsystems.tronkit.models.TransferAssetContract
import io.horizontalsystems.tronkit.models.TransferContract
import io.horizontalsystems.tronkit.models.TriggerSmartContract
import io.horizontalsystems.tronkit.models.UnfreezeBalanceV2Contract
import io.horizontalsystems.tronkit.models.Unknown
import io.horizontalsystems.tronkit.models.VoteWitnessContract
import io.horizontalsystems.tronkit.models.WithdrawBalanceContract

val Contract.label: String
    get() = when (this) {
        is AssetIssueContract -> "Issue TRC10 token"
        is CreateSmartContract -> "Create Smart Contract"
        is FreezeBalanceV2Contract -> "TRX Stake 2.0"
        is TransferAssetContract -> "Transfer TRC10 Token"
        is TransferContract -> "TRX Transfer"
        is TriggerSmartContract -> "Trigger Smart Contract"
        is UnfreezeBalanceV2Contract -> "TRX Unstake 2.0"
        is VoteWitnessContract -> "Vote"
        is WithdrawBalanceContract -> "Claim Rewards"
        is Unknown -> this.javaClass.simpleName
    }
