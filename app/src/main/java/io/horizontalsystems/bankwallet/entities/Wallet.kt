package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.core.protocolType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Wallet(
    val configuredToken: ConfiguredToken,
    val account: Account
) : Parcelable {
    private val blockchain: TransactionSource.Blockchain
        get() = when (token.blockchainType) {
            BlockchainType.Bitcoin -> TransactionSource.Blockchain.Bitcoin
            BlockchainType.BitcoinCash -> TransactionSource.Blockchain.BitcoinCash
            BlockchainType.Litecoin -> TransactionSource.Blockchain.Litecoin
            BlockchainType.Dash -> TransactionSource.Blockchain.Dash
            BlockchainType.Zcash -> TransactionSource.Blockchain.Zcash
            BlockchainType.Ethereum,
            BlockchainType.BinanceSmartChain,
            BlockchainType.Polygon,
            BlockchainType.Optimism,
            BlockchainType.ArbitrumOne -> TransactionSource.Blockchain.Evm(token.blockchain)
            BlockchainType.BinanceChain -> {
                when (val tokenType = token.type) {
                    TokenType.Native -> TransactionSource.Blockchain.Bep2("BNB")
                    is TokenType.Bep2 -> TransactionSource.Blockchain.Bep2(tokenType.symbol)
                    else -> throw IllegalArgumentException("Unsupported coin may not have transactions to show")
                }
            }
            is BlockchainType.Unsupported -> throw IllegalArgumentException("Unsupported coin may not have transactions to show")
        }

    val token
        get() = configuredToken.token

    val coinSettings
        get() = configuredToken.coinSettings

    val coin
        get() = token.coin

    val decimal
        get() = token.decimals

    val badge
        get() = when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.Litecoin,
            -> coinSettings.derivation?.value?.uppercase()
            BlockchainType.BitcoinCash -> coinSettings.bitcoinCashCoinType?.value?.uppercase()
            else -> token.protocolType
        }

    val transactionSource get() = TransactionSource(blockchain, account, coinSettings)

    constructor(token: Token, account: Account) : this(ConfiguredToken(token), account)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return configuredToken == other.configuredToken && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(configuredToken, account)
    }
}
