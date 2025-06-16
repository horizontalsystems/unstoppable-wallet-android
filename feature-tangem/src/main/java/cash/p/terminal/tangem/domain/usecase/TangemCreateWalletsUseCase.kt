package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TangemCreateWalletsUseCase(
    internal val tangemSdkManager: TangemSdkManager
) {
    suspend operator fun invoke(
        scanResponse: ScanResponse,
        shouldReset: Boolean
    ) = withContext(Dispatchers.IO) {
        tangemSdkManager.createProductWallet(
            scanResponse,
            shouldReset
        )
    }
}