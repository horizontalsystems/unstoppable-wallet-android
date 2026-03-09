package cash.p.terminal.network.quickex.data.entity

import kotlinx.serialization.Serializable

@Serializable
internal class NewTransactionQuickexResponseDto(
    val orderId: Long,
    val pair: PairDto,
    val depositAddress: DepositAddressDto,
    val claimedDepositAmount: String,
    val amountToGet: String,
    val claimedPublicRate: ClaimedPublicRateDto
)

@Serializable
internal class DepositAddressDto(
    val instrument: InstrumentInfoDto,
    val depositAddress: String,
    val depositAddressMemo: String? = null
)

@Serializable
internal class PairDto(
    val instrumentFrom: InstrumentInfoDto,
    val instrumentTo: InstrumentInfoDto
)

@Serializable
internal class InstrumentInfoDto(
    val currencyTitle: String,
    val networkTitle: String
)

@Serializable
internal class ClaimedPublicRateDto(
    val price: String,
    val claimedAmountToReceive: String,
    val finalNetworkFeeAmount: String?
)
