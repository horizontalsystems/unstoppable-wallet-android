package cash.p.terminal.core.managers

import cash.p.terminal.feature.miniapp.domain.usecase.GetTonAddressUseCase
import cash.p.terminal.wallet.Account
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.withContext

class GetTonAddressUseCaseImpl(
    private val tonKitManager: TonKitManager,
    private val dispatcherProvider: DispatcherProvider
) : GetTonAddressUseCase {

    override suspend fun getAddress(account: Account): String =
        withContext(dispatcherProvider.io) {
            tonKitManager.getTonWallet(
                account,
                BlockchainType.Ton,
            ).address.toUserFriendly(false)
        }
}
