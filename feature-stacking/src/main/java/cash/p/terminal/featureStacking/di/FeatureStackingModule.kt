package cash.p.terminal.featureStacking.di

import cash.p.terminal.featureStacking.ui.PirateInvestmentChartService
import cash.p.terminal.featureStacking.ui.calculatorScreen.CalculatorViewModel
import cash.p.terminal.featureStacking.ui.cosantaCoinScreen.CosantaCoinChartViewModel
import cash.p.terminal.featureStacking.ui.cosantaCoinScreen.CosantaCoinViewModel
import cash.p.terminal.featureStacking.ui.pirateCoinScreen.PirateCoinChartViewModel
import cash.p.terminal.featureStacking.ui.pirateCoinScreen.PirateCoinViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val featureStackingModule = module {
    viewModelOf(::StackingViewModel)
    viewModel {
        PirateCoinViewModel(
            walletManager = get(),
            adapterManager = get(),
            piratePlaceRepository = get(),
            balanceService = get(named("wallet")),
            accountManager = get(),
            marketKitWrapper = get()
        )
    }
    viewModel {
        CosantaCoinViewModel(
            walletManager = get(),
            adapterManager = get(),
            piratePlaceRepository = get(),
            balanceService = get(named("wallet")),
            accountManager = get(),
            marketKitWrapper = get()
        )
    }
    viewModelOf(::PirateCoinChartViewModel)
    viewModelOf(::CosantaCoinChartViewModel)
    viewModel {
        CalculatorViewModel(
            balanceService = get(named("wallet")),
            piratePlaceRepository = get(),
            numberFormatter = get()
        )
    }
    factoryOf(::PirateInvestmentChartService)
}