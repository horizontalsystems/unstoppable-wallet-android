package cash.p.terminal.core.usecase

import androidx.room.withTransaction
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.managers.WalletActivator
import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.totalSignedHashes
import cash.p.terminal.tangem.domain.usecase.BuildHardwarePublicKeyUseCase
import cash.p.terminal.tangem.domain.usecase.ICreateHardwareWalletUseCase
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.entities.TokenQuery

internal class CreateHardwareWalletUseCase(
    private val hardwarePublicKeyStorage: IHardwarePublicKeyStorage,
    private val accountManager: IAccountManager,
    private val appDatabase: AppDatabase
) : ICreateHardwareWalletUseCase {

    private val accountFactory: IAccountFactory = App.accountFactory
    private val walletActivator: WalletActivator = App.walletActivator

    @OptIn(ExperimentalStdlibApi::class)
    override suspend operator fun invoke(
        accountName: String,
        scanResponse: ScanResponse
    ): AccountType.HardwareCard {
        val accountType = AccountType.HardwareCard(
            cardId = scanResponse.card.cardId,
            backupCardsCount = scanResponse.card.backupStatus?.linkedCardsCount ?: 0,
            walletPublicKey = scanResponse.card.cardPublicKey.toHexString(),
            signedHashes = scanResponse.card.totalSignedHashes()
        )
        val account = accountFactory.account(
            name = accountName,
            type = accountType,
            origin = AccountOrigin.Created,
            backedUp = false,
            fileBackedUp = false,
        )

        val defaultTokens = TangemConfig.getDefaultTokens

        val blockchainTypes = defaultTokens.distinct()
        val publicKeys =
            BuildHardwarePublicKeyUseCase().invoke(scanResponse, account.id, blockchainTypes)
        appDatabase.withTransaction {
            hardwarePublicKeyStorage.save(publicKeys)
            activateDefaultWallets(
                account = account,
                tokenQueries = defaultTokens.filter { defaultToken ->
                    publicKeys.find { it.blockchainType == defaultToken.blockchainType.uid } != null
                }
            )
            accountManager.save(account = account, updateActive = false)
        }

        accountManager.setActiveAccountId(account.id)
        return accountType
    }

    private suspend fun activateDefaultWallets(
        account: Account,
        tokenQueries: List<TokenQuery> = TangemConfig.getDefaultTokens
    ) = walletActivator.activateWalletsSuspended(account, tokenQueries)
}