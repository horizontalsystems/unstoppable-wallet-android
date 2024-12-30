package cash.p.terminal.network.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InvestmentDataDto(
    val id: Int,
    val chain: String,
    val source: String,
    val address: String,
    val balance: String,
    @SerialName("unrealized_value")
    val unrealizedValue: String,
    val mint: String,
    @SerialName("balance_price")
    val balancePrice: Map<String, String>,
    @SerialName("unrealized_value_price")
    val unrealizedValuePrice: Map<String, String>,
    @SerialName("mint_price")
    val mintPrice: Map<String, String>
)