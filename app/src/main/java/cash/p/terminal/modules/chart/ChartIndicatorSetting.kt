package cash.p.terminal.modules.chart

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity
data class ChartIndicatorSetting(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: IndicatorType,
    val extraData: Map<String, String>,
    val enabled: Boolean
) {
    enum class IndicatorType {
        MA, RSI, MACD
    }

    val pointsCount: Int
        get() = when (type) {
            IndicatorType.MA -> extraData["period"]?.toInt() ?: 0
            IndicatorType.RSI -> 0
            IndicatorType.MACD -> 0
        }
}

@Dao
interface ChartIndicatorSettingsDao {
    @Query("SELECT COUNT(*) FROM ChartIndicatorSetting")
    fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(settings: List<ChartIndicatorSetting>)

    @Query("SELECT * FROM ChartIndicatorSetting")
    fun getAll(): Flow<List<ChartIndicatorSetting>>

    @Query("SELECT * FROM ChartIndicatorSetting WHERE enabled = 1")
    fun getEnabled(): List<ChartIndicatorSetting>

    @Query("UPDATE ChartIndicatorSetting SET enabled = 1 WHERE id = :indicatorId")
    fun enableIndicator(indicatorId: String)

    @Query("UPDATE ChartIndicatorSetting SET enabled = 0 WHERE id = :indicatorId")
    fun disableIndicator(indicatorId: String)

    companion object {
        fun defaultData(): List<ChartIndicatorSetting> {
            return listOf(
                ChartIndicatorSetting(
                    id = "ma1",
                    name = "SMA",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    extraData = mapOf(
                        "period" to "20",
                        "maType" to "SMA",
                        "color" to "#FFA800",
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "ma2",
                    name = "WMA",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    extraData = mapOf(
                        "period" to "20",
                        "maType" to "WMA",
                        "color" to "#4A98E9",
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "ma3",
                    name = "EMA",
                    type = ChartIndicatorSetting.IndicatorType.MA,
                    extraData = mapOf(
                        "period" to "20",
                        "maType" to "EMA",
                        "color" to "#BF5AF2",
                    ),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "rsi",
                    name = "RSI",
                    type = ChartIndicatorSetting.IndicatorType.RSI,
                    extraData = mapOf(),
                    enabled = true,
                ),
                ChartIndicatorSetting(
                    id = "macd",
                    name = "MACD",
                    type = ChartIndicatorSetting.IndicatorType.MACD,
                    extraData = mapOf(),
                    enabled = false
                ),
            )

        }
    }
}