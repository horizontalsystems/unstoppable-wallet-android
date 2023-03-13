package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class ProChartPointDataRaw(
    val timestamp: Long,
    val count: Int?,
    val volume: BigDecimal?
) {

    companion object {

        fun volumePoints(rawChartPoints: List<ProChartPointDataRaw>): List<ChartPoint> =
            rawChartPoints.mapNotNull { raw ->
                raw.volume?.let {
                    ChartPoint(
                        it,
                        raw.timestamp,
                        mapOf()
                    )
                }
            }

        fun countPoints(rawChartPoints: List<ProChartPointDataRaw>): List<ChartPoint> =
            rawChartPoints.mapNotNull { raw ->
                raw.count?.let {
                    ChartPoint(
                        it.toBigDecimal(),
                        raw.timestamp,
                        mapOf()
                    )
                }
            }

    }

}

data class DexLiquiditiesResponse(
    val platforms: List<String>,
    val liquidity: List<ProChartPointDataRaw>
) {

    val volumePoints: List<ChartPoint>
        get() = ProChartPointDataRaw.volumePoints(liquidity)

}

data class DexVolumesResponse(
    val platforms: List<String>,
    val volumes: List<ProChartPointDataRaw>
) {

    val volumePoints: List<ChartPoint>
        get() = ProChartPointDataRaw.volumePoints(volumes)

}

data class TransactionsDataResponse(
    val platforms: List<String>,
    val transactions: List<ProChartPointDataRaw>
) {

    val volumePoints: List<ChartPoint>
        get() = ProChartPointDataRaw.volumePoints(transactions)

    val countPoints: List<ChartPoint>
        get() = ProChartPointDataRaw.countPoints(transactions)

}

data class ActiveAddressesDataResponse(
    val platforms: List<String>,
    val addresses: List<ProChartPointDataRaw>
) {

    val countPoints: List<ChartPoint>
        get() = ProChartPointDataRaw.countPoints(addresses)

}

data class Analytics(
    @SerializedName("cex_volume")
    val cexVolume: ExVolume?,
    @SerializedName("dex_volume")
    val dexVolume: ExVolume?,
    @SerializedName("dex_liquidity")
    val dexLiquidity: DexLiquidity?,
    val addresses: Addresses?,
    val transactions: Transactions?,
    val revenue: Revenue?,
    val tvl: Tvl?,
    val reports: Int?,
    @SerializedName("funds_invested")
    val fundsInvested: BigDecimal?,
    val treasuries: BigDecimal?,
    val holders: List<HolderBlockchain>?,
) {

    data class ExVolume(
        @SerializedName("rank_30d")
        val rank30d: Int,
        val points: List<VolumePoint>,
    ) {
        fun chartPoints(): List<ChartPoint> {
            return points.map {
                ChartPoint(
                    it.volume,
                    it.timestamp,
                    mapOf()
                )
            }
        }
    }

    data class DexLiquidity(
        val rank: Int,
        val points: List<VolumePoint>,
    ) {
        fun chartPoints(): List<ChartPoint> {
            return points.map {
                ChartPoint(
                    it.volume,
                    it.timestamp,
                    mapOf()
                )
            }
        }
    }

    data class Addresses(
        @SerializedName("rank_30d")
        val rank30d: Int,
        @SerializedName("count_30d")
        val count30d: Int,
        val points: List<CountPoint>,
    ) {
        fun chartPoints(): List<ChartPoint> {
            return points.map {
                ChartPoint(
                    it.count,
                    it.timestamp,
                    mapOf()
                )
            }
        }
    }

    data class Transactions(
        @SerializedName("rank_30d")
        val rank30d: Int,
        @SerializedName("volume_30d")
        val volume30d: BigDecimal,
        val points: List<CountPoint>,
    ) {
        fun chartPoints(): List<ChartPoint> {
            return points.map {
                ChartPoint(
                    it.count,
                    it.timestamp,
                    mapOf()
                )
            }
        }
    }

    data class Tvl(
        val rank: Int,
        val ratio: BigDecimal,
        val points: List<TvlPoint>,
    ) {
        fun chartPoints(): List<ChartPoint> {
            return points.map {
                ChartPoint(
                    it.tvl,
                    it.timestamp,
                    mapOf()
                )
            }
        }
    }

    data class CountPoint(
        val count: BigDecimal,
        val timestamp: Long,
    )

    data class TvlPoint(
        val tvl: BigDecimal,
        val timestamp: Long,
    )

    data class VolumePoint(
        val volume: BigDecimal,
        val timestamp: Long,
    )

    data class HolderBlockchain(
        @SerializedName("blockchain_uid")
        val blockchainUid: String,
        @SerializedName("holders_count")
        val holdersCount: BigDecimal,
    )

    data class Revenue(
        @SerializedName("rank_30d")
        val rank30d: Int,
        @SerializedName("value_30d")
        val value30d: BigDecimal,
    )
}

data class AnalyticsPreview(
    @SerializedName("cex_volume")
    val cexVolume: VolumePreview?,
    @SerializedName("dex_volume")
    val dexVolume: VolumePreview?,
    @SerializedName("dex_liquidity")
    val dexLiquidity: LiquidityPreview?,
    val addresses: VolumePreview?,
    val transactions: TransactionPreview?,
    val revenue: RevenuePreview?,
    val tvl: TvlPreview?,
    val reports: Boolean = false,
    @SerializedName("funds_invested")
    val fundsInvested: Boolean = false,
    val treasuries: Boolean = false,
    val holders: Boolean = false,
) {

    data class VolumePreview(
        @SerializedName("rank_30d")
        val rank30d: Boolean = false,
        val points: Boolean = false,
    )

    data class LiquidityPreview(
        val rank: Boolean = false,
        val points: Boolean = false,
    )

    data class TransactionPreview(
        @SerializedName("rank_30d")
        val rank30d: Boolean = false,
        @SerializedName("volume_30d")
        val volume30d: Boolean = false,
        val points: Boolean = false,
    )

    data class RevenuePreview(
        @SerializedName("rank_30d")
        val rank30d: Boolean = false,
        @SerializedName("value_30d")
        val value30d: Boolean = false,
    )

    data class TvlPreview(
        val rank: Boolean = false,
        val ratio: Boolean = false,
        val points: Boolean = false,
    )
}