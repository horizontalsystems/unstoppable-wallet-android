package cash.p.terminal.featureStacking.ui.cosantaCoinScreen

import cash.p.terminal.featureStacking.ui.stackingCoinScreen.StackingCoinViewModel
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.network.domain.repository.PiratePlaceRepository
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.balance.BalanceService

internal class CosantaCoinViewModel(
    walletManager: IWalletManager,
    adapterManager: IAdapterManager,
    piratePlaceRepository: PiratePlaceRepository,
    balanceService: BalanceService,
    accountManager: IAccountManager,
    marketKitWrapper: MarketKitWrapper,
): StackingCoinViewModel(
    walletManager = walletManager,
    adapterManager = adapterManager,
    piratePlaceRepository = piratePlaceRepository,
    balanceService = balanceService,
    accountManager = accountManager,
    marketKitWrapper = marketKitWrapper
) {
    override val minStackingAmount = 1
    override val stackingType: StackingType = StackingType.COSANTA
}