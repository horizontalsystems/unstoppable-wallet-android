package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import cash.p.terminal.featureStacking.ui.stackingCoinScreen.StackingCoinViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.pirate.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.balance.BalanceService
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
import io.horizontalsystems.core.BackgroundManager

internal class PirateCoinViewModel(
    walletManager: IWalletManager,
    adapterManager: IAdapterManager,
    piratePlaceRepository: PiratePlaceRepository,
    balanceService: BalanceService,
    accountManager: IAccountManager,
    marketKitWrapper: MarketKitWrapper,
    balanceHiddenManager: IBalanceHiddenManager,
    backgroundManager: BackgroundManager
) : StackingCoinViewModel(
    walletManager = walletManager,
    adapterManager = adapterManager,
    piratePlaceRepository = piratePlaceRepository,
    balanceService = balanceService,
    accountManager = accountManager,
    marketKitWrapper = marketKitWrapper,
    balanceHiddenManager = balanceHiddenManager,
    backgroundManager = backgroundManager
) {
    override val minStackingAmount = 100
    override val stackingType: StackingType = StackingType.PCASH
}