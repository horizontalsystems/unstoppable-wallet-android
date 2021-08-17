package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.modules.transactions.FilterAdapter
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class Wallet(
    val configuredCoin: ConfiguredCoin,
    val account: Account
) : Parcelable, FilterAdapter.FilterItem(configuredCoin.coin.code) {
    private val blockchain: TransactionSource.Blockchain
        get() = when (val type = coin.type) {
            CoinType.Bitcoin -> TransactionSource.Blockchain.Bitcoin
            CoinType.BitcoinCash -> TransactionSource.Blockchain.BitcoinCash
            CoinType.Dash -> TransactionSource.Blockchain.Dash
            CoinType.Litecoin -> TransactionSource.Blockchain.Litecoin
            CoinType.Ethereum -> TransactionSource.Blockchain.Ethereum
            CoinType.BinanceSmartChain -> TransactionSource.Blockchain.Ethereum
            CoinType.Zcash -> TransactionSource.Blockchain.Zcash
            is CoinType.Bep2 -> TransactionSource.Blockchain.Bep2(type.symbol)
            is CoinType.Erc20 -> TransactionSource.Blockchain.Ethereum
            is CoinType.Bep20 -> TransactionSource.Blockchain.BinanceSmartChain
            is CoinType.Unsupported -> throw IllegalArgumentException("Unsupported coin may not have transactions to show")
        }

    val coin get() = configuredCoin.coin
    val transactionSource get() = TransactionSource(blockchain, account, configuredCoin.settings)

    constructor(coin: Coin, account: Account) : this(ConfiguredCoin(coin), account)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return configuredCoin == other.configuredCoin && account == other.account
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(configuredCoin, account)
    }
}
