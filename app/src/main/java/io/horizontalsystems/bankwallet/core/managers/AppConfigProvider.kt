package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Currency

class AppConfigProvider : IAppConfigProvider {
    override val ipfsUrl = "https://ipfs.io/ipns/QmXTJZBMMRmBbPun6HFt3tmb3tfYF2usLPxFoacL7G5uMX/"

    override val fiatDecimal: Int = 2
    override val maxDecimal: Int = 8

    override val testMode: Boolean = BuildConfig.testMode

    override val currencies: List<Currency> = listOf(
            Currency(code = "USD", symbol = "\u0024"),
            Currency(code = "EUR", symbol = "\u20AC"),
            Currency(code = "GBP", symbol = "\u00A3"),
            Currency(code = "JPY", symbol = "\u00A5"),
            Currency(code = "AUD", symbol = "\u20B3"),
            Currency(code = "CAD", symbol = "\u0024"),
            Currency(code = "CHF", symbol = "\u20A3"),
            Currency(code = "CNY", symbol = "\u00A5"),
            Currency(code = "KRW", symbol = "\u20AE"),
            Currency(code = "RUB", symbol = "\u20BD"),
            Currency(code = "TRY", symbol = "\u20BA")
    )

    override val localizations: List<String>
        get() {
            val coinsString = App.instance.getString(R.string.localizations)
            return coinsString.split(",")
        }

    override val defaultCoins: List<Coin>
        get() {
            val coins = mutableListOf<Coin>()
            coins.add(Coin("Bitcoin", "BTC", CoinType.Bitcoin))
            coins.add(Coin("Bitcoin Cash", "BCH", CoinType.BitcoinCash))
            coins.add(Coin("Ethereum", "ETH", CoinType.Ethereum))
            return coins
        }

    override val erc20tokens: List<Coin> = listOf(
            Coin("0x", "ZRX", CoinType.Erc20("0xE41d2489571d322189246DaFA5ebDe1F4699F498", 18)),
            Coin("Aurora DAO", "AURA", CoinType.Erc20("0xCdCFc0f66c522Fd086A1b725ea3c0Eeb9F9e8814", 18)),
            Coin("Bancor", "BNT", CoinType.Erc20("0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C", 18)),
            Coin("Basic Attention Token", "BAT", CoinType.Erc20("0x0D8775F648430679A709E98d2b0Cb6250d2887EF", 18)),
            Coin("Binance Coin", "BNB", CoinType.Erc20("0xB8c77482e45F1F44dE1745F52C74426C631bDD52", 18)),
            Coin("ChainLink", "LINK", CoinType.Erc20("0x514910771AF9Ca656af840dff83E8264EcF986CA", 18)),
            Coin("Crypto.com", "MCO", CoinType.Erc20("0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d", 8)),
            Coin("Dai", "DAI", CoinType.Erc20("0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359", 18)),
            Coin("Decentraland", "MANA", CoinType.Erc20("0x0F5D2fB29fb7d3CFeE444a200298f468908cC942", 18)),
            Coin("Digix DAO", "DGD", CoinType.Erc20("0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A", 9)),
            Coin("Digix Gold", "DGX", CoinType.Erc20("0x4f3AfEC4E5a3F2A6a1A411DEF7D7dFe50eE057bF", 9)),
            Coin("EnjinCoin", "ENJ", CoinType.Erc20("0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c", 18)),
            Coin("Gemini Dollar", "GUSD", CoinType.Erc20("0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd", 2)),
            Coin("Golem", "GNT", CoinType.Erc20("0xa74476443119A942dE498590Fe1f2454d7D4aC0d", 18)),
            Coin("Huobi Token", "HT", CoinType.Erc20("0x6f259637dcD74C767781E37Bc6133cd6A68aa161", 18)),
            Coin("IDEX Membership", "IDXM", CoinType.Erc20("0xCc13Fc627EFfd6E35D2D2706Ea3C4D7396c610ea", 8)),
            Coin("KuCoin Shares", "KCS", CoinType.Erc20("0x039B5649A59967e3e936D7471f9c3700100Ee1ab", 6)),
            Coin("Kyber Network", "KNC", CoinType.Erc20("0xdd974D5C2e2928deA5F71b9825b8b646686BD200", 18)),
            Coin("Loom", "LOOM", CoinType.Erc20("0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0", 18)),
            Coin("Maker", "MKR", CoinType.Erc20("0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2", 18)),
            Coin("Mithril", "MITH", CoinType.Erc20("0x3893b9422Cd5D70a81eDeFfe3d5A1c6A978310BB", 18)),
            Coin("Nexo", "NEXO", CoinType.Erc20("0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206", 18)),
            Coin("OmiseGO", "OMG", CoinType.Erc20("0xd26114cd6EE289AccF82350c8d8487fedB8A0C07", 18)),
            Coin("Paxos Standard", "PAX", CoinType.Erc20("0x8E870D67F660D95d5be530380D0eC0bd388289E1", 18)),
            Coin("Polymath", "POLY", CoinType.Erc20("0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC", 18)),
            Coin("Populous", "PPT", CoinType.Erc20("0xd4fa1460F537bb9085d22C7bcCB5DD450Ef28e3a", 8)),
            Coin("Pundi X", "NPXS", CoinType.Erc20("0xA15C7Ebe1f07CaF6bFF097D8a589fb8AC49Ae5B3", 18)),
            Coin("Reputation (Augur)", "REP", CoinType.Erc20("0x1985365e9f78359a9B6AD760e32412f4a445E862", 18)),
            Coin("Revain", "R", CoinType.Erc20("0x48f775EFBE4F5EcE6e0DF2f7b5932dF56823B990", 0)),
            Coin("STASIS EURS", "EURS", CoinType.Erc20("0xdB25f211AB05b1c97D595516F45794528a807ad8", 2)),
            Coin("Status", "SNT", CoinType.Erc20("0x744d70FDBE2Ba4CF95131626614a1763DF805B9E", 18)),
            Coin("TrueUSD", "TUSD", CoinType.Erc20("0x0000000000085d4780B73119b644AE5ecd22b376", 18)),
            Coin("USD Coin", "USDC", CoinType.Erc20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6)),
            Coin("Walton", "WTC", CoinType.Erc20("0xb7cB1C96dB6B22b0D3d9536E0108d062BD488F74", 18)),
            Coin("WAX Token", "WAX", CoinType.Erc20("0x39Bb259F66E1C59d5ABEF88375979b4D20D98022", 8)),
            Coin("Zilliqa", "ZIL", CoinType.Erc20("0x05f4a42e251f2d52b8ed15E9FEdAacFcEF1FAD27", 12))
    )
}
