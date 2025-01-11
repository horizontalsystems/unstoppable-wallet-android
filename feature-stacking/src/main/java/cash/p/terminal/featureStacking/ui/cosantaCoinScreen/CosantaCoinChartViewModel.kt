package cash.p.terminal.featureStacking.ui.cosantaCoinScreen

import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import cash.p.terminal.featureStacking.ui.stackingCoinScreen.StackingCoinChartViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingType

internal class CosantaCoinChartViewModel(service: PirateInvestmentChartService) :
    StackingCoinChartViewModel(service = service) {
    override val coinCode: String = StackingType.COSANTA.value
}