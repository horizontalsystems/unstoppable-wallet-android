package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.wallet.AccountType

interface ICreateHardwareWalletUseCase {
    suspend operator fun invoke(
        accountName: String,
        scanResponse: ScanResponse
    ): AccountType.HardwareCard
}