package io.horizontalsystems.bankwallet.modules.chart

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class ChartIndicatorSetting(
    @PrimaryKey
    val id: String,
    val type: IndicatorType,
    val index: Int = 1,
    val extraData: Map<String, String?>,
    val defaultData: Map<String, String?>,
    val enabled: Boolean
) {
    enum class IndicatorType {
        MA, RSI, MACD
    }

    val name: String
        get() = when (type) {
            IndicatorType.MA -> "MA $index"
            IndicatorType.RSI -> "RSI"
            IndicatorType.MACD -> "MACD"
        }

    val pointsCount: Int
        get() = when (type) {
            IndicatorType.MA -> {
                getTypedDataMA().period
            }
            IndicatorType.RSI -> {
                getTypedDataRsi().period
            }
            IndicatorType.MACD -> {
                getTypedDataMacd().slow + getTypedDataMacd().signal
            }
        }

    fun getTypedDataMA(): ChartIndicatorDataMa {
        check(type == IndicatorType.MA)

        val period = extraData["period"]?.toIntOrNull() ?: defaultData["period"]?.toIntOrNull() ?: 20
        val maType = extraData["maType"] ?: defaultData["maType"] ?: "SMA"
        val color = extraData["color"]?.toLongOrNull() ?: defaultData["color"]?.toLongOrNull() ?: 0xFFFFA800

        return ChartIndicatorDataMa(
            period = period,
            maType = maType,
            color = color
        )
    }

    fun getTypedDataRsi(): ChartIndicatorDataRsi {
        check(type == IndicatorType.RSI)

        val period = extraData["period"]?.toIntOrNull() ?: defaultData["period"]?.toIntOrNull() ?: 12

        return ChartIndicatorDataRsi(
            period = period
        )
    }

    fun getTypedDataMacd(): ChartIndicatorDataMacd {
        check(type == IndicatorType.MACD)

        val fast = extraData["fast"]?.toIntOrNull() ?: defaultData["fast"]?.toIntOrNull() ?: 12
        val slow = extraData["slow"]?.toIntOrNull() ?: defaultData["slow"]?.toIntOrNull() ?: 26
        val signal = extraData["signal"]?.toIntOrNull() ?: defaultData["signal"]?.toIntOrNull() ?: 9

        return ChartIndicatorDataMacd(
            fast = fast,
            slow = slow,
            signal = signal,
        )
    }
}

data class ChartIndicatorDataMa(val period: Int, val maType: String, val color: Long)
data class ChartIndicatorDataRsi(val period: Int)
data class ChartIndicatorDataMacd(val fast: Int, val slow: Int, val signal: Int)

@Dao
interface ChartIndicatorSettingsDao {
    @Query("SELECT COUNT(*) FROM ChartIndicatorSetting")
    fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(settings: List<ChartIndicatorSetting>)

    @Query("SELECT * FROM ChartIndicatorSetting")
    fun getAll(): Flow<List<ChartIndicatorSetting>>

    @Query("SELECT * FROM ChartIndicatorSetting")
    fun getAllBlocking(): List<ChartIndicatorSetting>

    @Query("SELECT * FROM ChartIndicatorSetting WHERE enabled = 1")
    fun getEnabled(): List<ChartIndicatorSetting>

    @Query("UPDATE ChartIndicatorSetting SET enabled = 1 WHERE id = :indicatorId")
    fun enableIndicator(indicatorId: String)

    @Query("UPDATE ChartIndicatorSetting SET enabled = 0 WHERE id = :indicatorId")
    fun disableIndicator(indicatorId: String)

    @Query("SELECT * FROM ChartIndicatorSetting WHERE id = :id")
    fun get(id: String): ChartIndicatorSetting?

    @Update
    fun update(chartIndicatorSetting: ChartIndicatorSetting)

    companion object {
        fun defaultData(): List<ChartIndicatorSetting> {
            return listOf(
                ChartIndicatorSetting(
                    id = "ma1",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    index = 1,
                    extraData = mapOf(),
                    defaultData = mapOf(
                        "period" to "9",
                        "maType" to "EMA",
                        "color" to 0xFFFFA800.toString(),
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "ma2",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    index = 2,
                    extraData = mapOf(),
                    defaultData = mapOf(
                        "period" to "25",
                        "maType" to "EMA",
                        "color" to 0xFF4A98E9.toString(),
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "ma3",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    index = 3,
                    extraData = mapOf(),
                    defaultData = mapOf(
                        "period" to "50",
                        "maType" to "EMA",
                        "color" to 0xFFBF5AF2.toString(),
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "rsi",
                    type = ChartIndicatorSetting.IndicatorType.RSI,
                    extraData = mapOf(),
                    defaultData = mapOf(
                        "period" to "12",
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "macd",
                    type = ChartIndicatorSetting.IndicatorType.MACD,
                    extraData = mapOf(),
                    defaultData = mapOf(
                        "fast" to "12",
                        "slow" to "26",
                        "signal" to "9",
                    ),
                    enabled = false
                ),
            )

        }
    }
}