package cash.p.terminal.featureStacking.ui.pirateCoinScreen

import cash.p.terminal.featureStacking.ui.stackingCoinScreen.StackingCoinViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.balance.BalanceService

internal class PirateCoinViewModel(
    walletManager: IWalletManager,
    adapterManager: IAdapterManager,
    piratePlaceRepository: PiratePlaceRepository,
    balanceService: BalanceService,
    accountManager: IAccountManager,
    marketKitWrapper: MarketKitWrapper,
) : StackingCoinViewModel(
    walletManager = walletManager,
    adapterManager = adapterManager,
    piratePlaceRepository = piratePlaceRepository,
    balanceService = balanceService,
    accountManager = accountManager,
    marketKitWrapper = marketKitWrapper
) {
    override val minStackingAmount = 100
    override val stackingType: StackingType = StackingType.PCASH
}