package cash.p.terminal.wallet.useCases

import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType

class GetHardwarePublicKeyForWalletUseCase(
    private val hardwareWalletStorage: IHardwarePublicKeyStorage
) {

    suspend operator fun invoke(
        account: Account,
        blockchainType: BlockchainType,
        tokenType: TokenType
    ): HardwarePublicKey? {
        if (account.type !is AccountType.HardwareCard) {
            return null // Hardware accounts do not have public keys
        }

        return hardwareWalletStorage.getKey(account.id, blockchainType, tokenType)
    }

    suspend operator fun invoke(
        account: Account,
        tokenQuery: TokenQuery
    ) = this(
        account = account,
        blockchainType = tokenQuery.blockchainType,
        tokenType = tokenQuery.tokenType
    )

    suspend operator fun invoke(
        account: Account,
        token: Token
    ) = this(
        account = account,
        blockchainType = token.blockchainType,
        tokenType = token.type
    )
}