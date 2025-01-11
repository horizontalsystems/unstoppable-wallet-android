package cash.p.terminal.wallet.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class TopPair(
    val rank: Long,
    val base: String,
    @SerializedName("base_uid")
    val baseCoinUid: String?,
    @Transient
    val baseCoin: cash.p.terminal.wallet.entities.Coin?,
    val target: String,
    @SerializedName("target_uid")
    val targetCoinUid: String?,
    @Transient
    val targetCoin: cash.p.terminal.wallet.entities.Coin?,
    val price: BigDecimal?,
    val volume: BigDecimal,
    @SerializedName("market_name")
    val marketName: String?,
    @SerializedName("market_logo")
    val marketLogo: String?,
    @SerializedName("trade_url")
    val tradeUrl: String?,
)
