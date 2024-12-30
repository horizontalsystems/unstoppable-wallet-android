package cash.p.terminal.network.domain.repository

import cash.p.terminal.network.domain.enity.CalculatorData
import cash.p.terminal.network.domain.enity.InvestmentData
import cash.p.terminal.network.domain.enity.InvestmentGraphData
import cash.p.terminal.network.domain.enity.StakeData

interface PiratePlaceRepository {
    suspend fun getInvestmentData(address: String): InvestmentData
    suspend fun getInvestmentChart(address: String, period: String): InvestmentGraphData
    suspend fun getStakeData(address: String): StakeData
    suspend fun getCalculatorData(coin: String, amount: Double): CalculatorData
}