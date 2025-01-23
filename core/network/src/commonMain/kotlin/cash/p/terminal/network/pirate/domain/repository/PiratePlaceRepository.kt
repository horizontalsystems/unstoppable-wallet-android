package cash.p.terminal.network.pirate.domain.repository

import cash.p.terminal.network.pirate.domain.enity.CalculatorData
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin
import cash.p.terminal.network.pirate.domain.enity.InvestmentData
import cash.p.terminal.network.pirate.domain.enity.InvestmentGraphData
import cash.p.terminal.network.pirate.domain.enity.StakeData

interface PiratePlaceRepository {
    suspend fun getInvestmentData(coin: String, address: String): InvestmentData
    suspend fun getChangeNowCoinAssociation(uid: String): List<ChangeNowAssociatedCoin>
    suspend fun getInvestmentChart(coin: String, address: String, period: String): InvestmentGraphData
    suspend fun getStakeData(coin: String, address: String): StakeData
    suspend fun getCalculatorData(coin: String, amount: Double): CalculatorData
}