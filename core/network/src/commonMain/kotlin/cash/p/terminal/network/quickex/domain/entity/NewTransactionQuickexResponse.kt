package cash.p.terminal.network.quickex.domain.entity

import java.math.BigDecimal

data class NewTransactionQuickexResponse(
    val depositAddress: DepositAddress,
    val orderId: Long,
    val pair: Pair,
    val claimedDepositAmount: BigDecimal,
    val amountToGet: BigDecimal,
    val claimedPublicRate: ClaimedPublicRate,
)

data class DepositAddress(
    val instrument: InstrumentInfo,
    val depositAddress: String,
    val depositAddressMemo: String?,
)

data class Pair(
    val instrumentFrom: InstrumentInfo,
    val instrumentTo: InstrumentInfo
)

data class InstrumentInfo(
    val currencyTitle: String,
    val networkTitle: String
)

data class ClaimedPublicRate(
    val price: BigDecimal,
    val claimedAmountToReceive: BigDecimal,
    val finalNetworkFeeAmount: BigDecimal?
)
