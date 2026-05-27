package cash.p.terminal.premium.di

import cash.p.terminal.premium.data.UserPremiumSettings
import cash.p.terminal.premium.data.database.PremiumDatabase
import cash.p.terminal.premium.data.repository.PremiumUserRepository
import cash.p.terminal.premium.domain.PremiumSettings
import cash.p.terminal.premium.domain.usecase.ActivateTrialPremiumUseCase
import cash.p.terminal.premium.domain.usecase.CheckAdapterPremiumBalanceUseCase
import cash.p.terminal.premium.domain.usecase.CheckAdapterPremiumBalanceUseCaseImpl
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCaseImpl
import cash.p.terminal.premium.domain.usecase.CheckTrialPremiumUseCase
import cash.p.terminal.premium.domain.usecase.GetBnbAddressUseCase
import cash.p.terminal.premium.domain.usecase.GetBnbAddressUseCaseImpl
import cash.p.terminal.premium.domain.usecase.SeedToEvmAddressUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

const val PREMIUM_IS_DEBUG_QUALIFIER = "premiumIsDebug"

val featurePremiumModule = module {
    // Database
    single { PremiumDatabase.create(get()) }
    single { get<PremiumDatabase>().premiumUserDao() }
    single { get<PremiumDatabase>().demoPremiumUserDao() }
    single { get<PremiumDatabase>().bnbPremiumAddressDao() }

    // Repositories
    factoryOf(::PremiumUserRepository)

    // Settings
    singleOf(::UserPremiumSettings) bind PremiumSettings::class

    // Use Cases
    singleOf(::CheckAdapterPremiumBalanceUseCaseImpl) bind CheckAdapterPremiumBalanceUseCase::class
    singleOf(::CheckPremiumUseCaseImpl) bind CheckPremiumUseCase::class
    singleOf(::GetBnbAddressUseCaseImpl) bind GetBnbAddressUseCase::class
    factoryOf(::SeedToEvmAddressUseCase)
    factory {
        CheckTrialPremiumUseCase(get(), get(), get(), get(named(PREMIUM_IS_DEBUG_QUALIFIER)))
    }
    factoryOf(::ActivateTrialPremiumUseCase)
}
