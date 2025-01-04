package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import cash.p.terminal.featureStacking.ui.stackingCoinScreen.StackingCoinChartViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingType

internal class PirateCoinChartViewModel(service: PirateInvestmentChartService) :
    StackingCoinChartViewModel(service = service) {
    override val coinCode: String = StackingType.PCASH.value
}