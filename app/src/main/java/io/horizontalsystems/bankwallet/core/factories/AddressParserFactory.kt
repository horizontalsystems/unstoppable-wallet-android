package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.IAddressParser
import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.marketkit.models.CoinType

class AddressParserFactory {
    fun parser(coinType: CoinType): IAddressParser {
        return when (coinType) {
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
            is CoinType.ArbitrumOne,
            is CoinType.Avalanche,
            is CoinType.Fantom,
            is CoinType.HarmonyShard0,
            is CoinType.HuobiToken,
            is CoinType.Iotex,
            is CoinType.Moonriver,
            is CoinType.OkexChain,
            is CoinType.PolygonPos,
            is CoinType.Solana,
            is CoinType.Sora,
            is CoinType.Tomochain,
            is CoinType.Xdai,
            is CoinType.Unsupported -> AddressParser("", false)
        }
    }

}
