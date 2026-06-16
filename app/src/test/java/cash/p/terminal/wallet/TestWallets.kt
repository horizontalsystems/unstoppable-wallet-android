package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.mockk

internal fun zcashTransparentWallet(
    account: Account = zcashMnemonicAccount(),
): Wallet {
    val token = Token(
        coin = Coin(uid = "zcash", name = "Zcash", code = "ZEC"),
        blockchain = Blockchain(
            type = BlockchainType.Zcash,
            name = "Zcash",
            eip3091url = null
        ),
        type = TokenType.AddressSpecTyped(TokenType.AddressSpecType.Transparent),
        decimals = 8,
    )
    return checkNotNull(WalletFactory(mockk(relaxed = true)).create(token, account, null))
}

internal fun zcashMnemonicAccount(id: String = "zcash-account") = Account(
    id = id,
    name = "Zcash Account",
    type = AccountType.Mnemonic(List(12) { "word$it" }, ""),
    origin = AccountOrigin.Created,
    level = 0,
    isBackedUp = true,
)
