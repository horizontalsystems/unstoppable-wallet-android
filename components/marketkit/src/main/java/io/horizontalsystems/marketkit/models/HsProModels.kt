package io.horizontalsystems.marketkit.models

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
