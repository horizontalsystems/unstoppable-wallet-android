package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAddressParser
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType

class AddressParserFactory {
    fun parser(coin: Coin): IAddressParser {
        return when (coin.type) {
            is CoinType.Bitcoin -> AddressParser("bitcoin", true)
            is CoinType.Litecoin -> AddressParser("litecoin", true)
            is CoinType.BitcoinCash -> AddressParser("bitcoincash", false)
            is CoinType.Dash -> AddressParser("dash", true)
            is CoinType.Ethereum -> AddressParser("ethereum", true)
            is CoinType.Erc20 -> AddressParser("", true)
            is CoinType.BinanceSmartChain -> AddressParser("", true)
            is CoinType.Bep20 -> AddressParser("", true)
            is CoinType.Bep2 -> AddressParser("binance", true)
            is CoinType.Zcash -> AddressParser("zcash", true)
            is CoinType.Unsupported -> AddressParser("", false)
        }
    }

}
