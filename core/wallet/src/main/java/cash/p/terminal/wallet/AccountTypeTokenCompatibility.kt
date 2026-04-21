package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType

fun AccountType.isCompatibleWith(blockchainType: BlockchainType, tokenType: TokenType): Boolean {
    return when (this) {
        is AccountType.MnemonicMonero -> {
            blockchainType == BlockchainType.Monero && tokenType == TokenType.Native
        }

        else -> true
    }
}
