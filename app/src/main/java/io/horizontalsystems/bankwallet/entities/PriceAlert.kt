package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.coinkit.models.CoinType


@Entity
class PriceAlert(
        @PrimaryKey val coinType: CoinType,
        val notificationCoinCode: String,
        val coinName: String,
        var changeState: ChangeState,
        var trendState: TrendState
) {

    enum class ChangeState(val value: String) {
        OFF("off"),
        PERCENT_2("2"),
        PERCENT_5("5"),
        PERCENT_10("10");

        companion object {
            fun valueOf(value: String?): ChangeState {
                return values().find { it.value == value } ?: OFF
            }
        }
    }

    enum class TrendState(val value: String) {
        OFF("off"),
        SHORT("short"),
        LONG("long");

        companion object {
            fun valueOf(value: String?): TrendState {
                return values().find { it.value == value } ?: OFF
            }
        }
    }
}
