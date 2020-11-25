package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.IAppConfigTestMode
import io.horizontalsystems.core.ILanguageConfigProvider
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class AppConfigProvider : IAppConfigProvider, ILanguageConfigProvider, IAppConfigTestMode {

    override val companyWebPageLink: String = "https://horizontalsystems.io"
    override val appWebPageLink: String = "https://unstoppable.money"
    override val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
    override val companyTwitterLink: String = "https://twitter.com/UnstoppableByHS"
    override val companyTelegramLink: String = "https://t.me/unstoppable_announcements"
    override val companyRedditLink: String = "https://reddit.com/r/UNSTOPPABLEWallet/"
    override val reportEmail = "hsdao@protonmail.ch"
    override val walletHelpTelegramGroup = "UnstoppableWallet"
    override val ipfsId = "QmXTJZBMMRmBbPun6HFt3tmb3tfYF2usLPxFoacL7G5uMX"
    override val ipfsMainGateway = "ipfs-ext.horizontalsystems.xyz"
    override val ipfsFallbackGateway = "ipfs.io"

    override val cryptoCompareApiKey = App.instance.getString(R.string.cryptoCompareApiKey)
    override val infuraProjectId = App.instance.getString(R.string.infuraProjectId)
    override val infuraProjectSecret = App.instance.getString(R.string.infuraSecretKey)
    override val etherscanApiKey = App.instance.getString(R.string.etherscanKey)
    override val guidesUrl = App.instance.getString(R.string.guidesUrl)
    override val faqUrl = App.instance.getString(R.string.faqUrl)
    override val fiatDecimal: Int = 2
    override val maxDecimal: Int = 8

    override val currencies: List<Currency> = listOf(
            Currency(code = "USD", symbol = "\u0024"),
            Currency(code = "EUR", symbol = "\u20AC"),
            Currency(code = "GBP", symbol = "\u00A3"),
            Currency(code = "JPY", symbol = "\u00A5")
    )

    override val featuredCoins: List<Coin>
        get() = listOf(
                defaultCoins[0],
                defaultCoins[1],
                defaultCoins[2],
                defaultCoins[3],
                defaultCoins[4],
                defaultCoins[5],
                defaultCoins[6],
                defaultCoins[7]
        )

    override val ethereumCoin: Coin
        get() = defaultCoins[2]

    override val defaultCoins: List<Coin> by lazy {
        listOf(
                Coin("BTC", "Bitcoin", "BTC", 8, CoinType.Bitcoin),
                Coin("LTC", "Litecoin", "LTC", 8, CoinType.Litecoin),
                Coin("ETH", "Ethereum", "ETH", 18, CoinType.Ethereum),
                Coin("BCH", "Bitcoin Cash", "BCH", 8, CoinType.BitcoinCash),
                Coin("DASH", "Dash", "DASH", 8, CoinType.Dash),
                Coin("BNB", "Binance Chain", "BNB", 8, CoinType.Binance("BNB")),
                Coin("ZEC", "Zcash", "ZEC", 8, CoinType.Zcash),
                Coin("EOS", "EOS", "EOS", 4, CoinType.Eos("eosio.token", "EOS")),
                Coin("ZRX", "0x Protocol", "ZRX", 18, CoinType.Erc20("0xE41d2489571d322189246DaFA5ebDe1F4699F498")),
                Coin("LEND", "Aave", "LEND", 18, CoinType.Erc20("0x80fB784B7eD66730e8b1DBd9820aFD29931aab03")),
                Coin("ADAI", "Aave DAI", "ADAI", 18, CoinType.Erc20("0xfC1E690f61EFd961294b3e1Ce3313fBD8aa4f85d")),
                Coin("AAVE", "Aave Token", "AAVE", 18, CoinType.Erc20("0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9")),
                Coin("ELF", "Aelf", "ELF", 18, CoinType.Erc20("0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e")),
                Coin("AST", "Air Swap", "AST", 4, CoinType.Erc20("0x27054b13b1b798b345b591a4d22e6562d47ea75a")),
                Coin("AKRO", "Akropolis", "AKRO", 18, CoinType.Erc20("0x8ab7404063ec4dbcfd4598215992dc3f8ec853d7")),
                Coin("AMN", "Amon", "AMN", 18, CoinType.Erc20("0x737f98ac8ca59f2c68ad658e3c3d8c8963e40a4c")),
                Coin("AMPL", "Ampleforth", "AMPL", 9, CoinType.Erc20("0xd46ba6d942050d489dbd938a2c909a5d5039a161")),
                Coin("ANKR", "Ankr Network", "ANKR", 8, CoinType.Binance("ANKR-E97")),
                Coin("ANT", "Aragon", "ANT", 18, CoinType.Erc20("0x960b236A07cf122663c4303350609A66A7B288C0")),
                Coin("ANJ", "Aragon Network Juror", "ANJ", 18, CoinType.Erc20("0xcD62b1C403fa761BAadFC74C525ce2B51780b184")),
                Coin("REP", "Augur", "REP", 18, CoinType.Erc20("0x1985365e9f78359a9B6AD760e32412f4a445E862")),
                Coin("BAL", "Balancer", "BAL", 18, CoinType.Erc20("0xba100000625a3754423978a60c9317c58a424e3D")),
                Coin("BNT", "Bancor", "BNT", 18, CoinType.Erc20("0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C")),
                Coin("BAND", "Band Token", "BAND", 18, CoinType.Erc20("0xba11d00c5f74255f56a5e366f4f77f5a186d7f55")),
                Coin("BAT", "Basic Attention Token", "BAT", 18, CoinType.Erc20("0x0D8775F648430679A709E98d2b0Cb6250d2887EF")),
                Coin("BNB-ERC20", "Binance ERC20", "BNB", 18, CoinType.Erc20("0xB8c77482e45F1F44dE1745F52C74426C631bDD52")),
                Coin("BUSD", "Binance USD", "BUSD", 8, CoinType.Binance("BUSD-BD1")),
                Coin("BTCB", "Bitcoin BEP2", "BTCB", 8, CoinType.Binance("BTCB-1DE")),
                Coin("CAS", "Cashaaa", "CAS", 8, CoinType.Binance("CAS-167")),
                Coin("LINK", "Chainlink", "LINK", 18, CoinType.Erc20("0x514910771AF9Ca656af840dff83E8264EcF986CA")),
                Coin("CVC", "Civic", "CVC", 8, CoinType.Erc20("0x41e5560054824ea6b0732e656e3ad64e20e94e45")),
                Coin("COMP", "Compound", "COMP", 18, CoinType.Erc20("0xc00e94cb662c3520282e6f5717214004a7f26888")),
                Coin("CRO", "Crypto.com Coin", "CRO", 8, CoinType.Erc20("0xA0b73E1Ff0B80914AB6fe0444E65848C4C34450b")),
                Coin("CRPT", "Crypterium", "CRPT", 8, CoinType.Binance("CRPT-8C9")),
                Coin("DAI", "Dai", "DAI", 18, CoinType.Erc20("0x6b175474e89094c44da98b954eedeac495271d0f")),
                Coin("MANA", "Decentraland", "MANA", 18, CoinType.Erc20("0x0F5D2fB29fb7d3CFeE444a200298f468908cC942")),
                Coin("DIA", "Dia", "DIA", 18, CoinType.Erc20("0x84ca8bc7997272c7cfb4d0cd3d55cd942b3c9419")),
                Coin("DGD", "DigixDAO", "DGD", 9, CoinType.Erc20("0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A")),
                Coin("DGX", "Digix Gold Token", "DGX", 9, CoinType.Erc20("0x4f3AfEC4E5a3F2A6a1A411DEF7D7dFe50eE057bF", minimumSendAmount = BigDecimal("0.001"))),
                Coin("DNT", "District0x", "DNT", 18, CoinType.Erc20("0x0abdace70d3790235af448c88547603b945604ea")),
                Coin("DOS", "DOSNetwork", "DOS", 8, CoinType.Binance("DOS-120")),
                Coin("DOS-ERC20", "DOSNetwork", "DOS", 18, CoinType.Erc20("0x0A913beaD80F321E7Ac35285Ee10d9d922659cB7")),
                Coin("ENJ", "EnjinCoin", "ENJ", 18, CoinType.Erc20("0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c")),
                Coin("EOSDT", "EOSDT", "EOSDT", 9, CoinType.Eos("eosdtsttoken", "EOSDT")),
                Coin("IQ", "Everipedia", "IQ", 3, CoinType.Eos("everipediaiq", "IQ")),
                Coin("GUSD", "Gemini Dollar", "GUSD", 2, CoinType.Erc20("0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd")),
                Coin("GNT", "Golem", "GNT", 18, CoinType.Erc20("0xa74476443119A942dE498590Fe1f2454d7D4aC0d")),
                Coin("GTO", "Gifto", "GTO", 8, CoinType.Binance("GTO-908")),
                Coin("HOT", "Holo", "HOT", 18, CoinType.Erc20("0x6c6EE5e31d828De241282B9606C8e98Ea48526E2")),
                Coin("HT", "Huobi Token", "HT", 18, CoinType.Erc20("0x6f259637dcD74C767781E37Bc6133cd6A68aa161")),
                Coin("IDEX", "IDEX", "IDEX", 18, CoinType.Erc20("0xB705268213D593B8FD88d3FDEFF93AFF5CbDcfAE")),
                Coin("IDXM", "IDEX Membership", "IDXM", 8, CoinType.Erc20("0xCc13Fc627EFfd6E35D2D2706Ea3C4D7396c610ea")),
                Coin("KCS", "KuCoin Shares", "KCS", 6, CoinType.Erc20("0x039B5649A59967e3e936D7471f9c3700100Ee1ab", minimumRequiredBalance = BigDecimal("0.001"))),
                Coin("KNC", "Kyber Network Crystal", "KNC", 18, CoinType.Erc20("0xdd974D5C2e2928deA5F71b9825b8b646686BD200")),
                Coin("LOOM", "Loom Network", "LOOM", 18, CoinType.Erc20("0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0")),
                Coin("LRC", "Loopring", "LRC", 18, CoinType.Erc20("0xEF68e7C694F40c8202821eDF525dE3782458639f")),
                Coin("MKR", "Maker", "MKR", 18, CoinType.Erc20("0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2")),
                Coin("MCO", "MCO", "MCO", 8, CoinType.Erc20("0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d")),
                Coin("MEETONE", "MEET.ONE", "MEETONE", 4, CoinType.Eos("eosiomeetone", "MEETONE")),
                Coin("MITH", "Mithril", "MITH", 18, CoinType.Erc20("0x3893b9422Cd5D70a81eDeFfe3d5A1c6A978310BB")),
                Coin("TKN", "Monolith", "TKN", 8, CoinType.Erc20("0xaaaf91d9b90df800df4f55c205fd6989c977e73a")),
                Coin("NEXO", "Nexo", "NEXO", 18, CoinType.Erc20("0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206")),
                Coin("NDX", "Newdex", "NDX", 4, CoinType.Eos("newdexissuer", "NDX")),
                Coin("NUT", "Native Utility Token", "NUT", 9, CoinType.Eos("eosdtnutoken", "NUT")),
                Coin("OMG", "OmiseGO", "OMG", 18, CoinType.Erc20("0xd26114cd6EE289AccF82350c8d8487fedB8A0C07")),
                Coin("ORBS", "Orbs", "ORBS", 18, CoinType.Erc20("0xff56Cc6b1E6dEd347aA0B7676C85AB0B3D08B0FA")),
                Coin("OXT", "Orchid", "OXT", 18, CoinType.Erc20("0x4575f41308EC1483f3d399aa9a2826d74Da13Deb")),
                Coin("PAR", "Parachute", "PAR", 18, CoinType.Erc20("0x1beef31946fbbb40b877a72e4ae04a8d1a5cee06")),
                Coin("PAX", "Paxos Standard", "PAX", 18, CoinType.Erc20("0x8E870D67F660D95d5be530380D0eC0bd388289E1")),
                Coin("PGL", "Prospectors Gold", "PGL", 4, CoinType.Eos("prospectorsg", "PGL")),
                Coin("PAXG", "PAX Gold", "PAXG", 18, CoinType.Erc20("0x45804880De22913dAFE09f4980848ECE6EcbAf78")),
                Coin("POLY", "Polymath", "POLY", 18, CoinType.Erc20("0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC")),
                Coin("PPT", "Populous", "PPT", 8, CoinType.Erc20("0xd4fa1460F537bb9085d22C7bcCB5DD450Ef28e3a")),
                Coin("PTI", "Paytomat", "PTI", 4, CoinType.Eos("ptitokenhome", "PTI")),
                Coin("NPXS", "Pundi X", "NPXS", 18, CoinType.Erc20("0xA15C7Ebe1f07CaF6bFF097D8a589fb8AC49Ae5B3")),
                Coin("RARI", "Rarible", "RARI", 18, CoinType.Erc20("0xfca59cd816ab1ead66534d82bc21e7515ce441cf")) ,
                Coin("REN", "Ren", "REN", 18, CoinType.Erc20("0x408e41876cccdc0f92210600ef50372656052a38")),
                Coin("RENBCH", "renBCH", "RENBCH", 8, CoinType.Erc20("0x459086f2376525bdceba5bdda135e4e9d3fef5bf")),
                Coin("RENBTC", "renBTC", "RENBTC", 8, CoinType.Erc20("0xeb4c2781e4eba804ce9a9803c67d0893436bb27d")),
                Coin("RENZEC", "renZEC", "RENZEC", 8, CoinType.Erc20("0x1c5db575e2ff833e46a2e9864c22f4b22e0b37c2")),
                Coin("R", "Revain", "R", 0, CoinType.Erc20("0x48f775EFBE4F5EcE6e0DF2f7b5932dF56823B990")),
                Coin("XRP", "Ripple BEP2", "XRP", 8, CoinType.Binance("XRP-BF2")),
                Coin("EURS", "STASIS EURO", "EURS", 2, CoinType.Erc20("0xdB25f211AB05b1c97D595516F45794528a807ad8")),
                Coin("SAI", "Sai", "SAI", 18, CoinType.Erc20("0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359")),
                Coin("SNT", "Status", "SNT", 18, CoinType.Erc20("0x744d70FDBE2Ba4CF95131626614a1763DF805B9E")),
                Coin("CHSB", "SwissBorg", "CHSB", 8, CoinType.Erc20("0xba9d4199fab4f26efe3551d490e3821486f135ba")),
                Coin("SNX", "Synthetix", "SNX", 18, CoinType.Erc20("0xC011a73ee8576Fb46F5E1c5751cA3B9Fe0af2a6F")),
                Coin("USDT", "Tether USD", "USDT", 6, CoinType.Erc20("0xdAC17F958D2ee523a2206206994597C13D831ec7")),
                Coin("TUSD", "TrueUSD", "TUSD", 18, CoinType.Erc20("0x0000000000085d4780B73119b644AE5ecd22b376")),
                Coin("SWAP", "TrustSwap", "SWAP", 18, CoinType.Erc20("0xCC4304A31d09258b0029eA7FE63d032f52e44EFe")),
                Coin("UNI", "Uniswap", "UNI", 18, CoinType.Erc20("0x1f9840a85d5af5bf1d1762f925bdaddc4201f984")),
                Coin("USDC", "USD Coin", "USDC", 6, CoinType.Erc20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")),
                Coin("WTC", "Waltonchain", "WTC", 18, CoinType.Erc20("0xb7cB1C96dB6B22b0D3d9536E0108d062BD488F74")),
                Coin("WBTC", "Wrapped Bitcoin", "WBTC", 8, CoinType.Erc20("0x2260fac5e5542a773aa44fbcfedf7c193bc2c599")),
                Coin("WETH", "Wrapped Ethereum", "WETH", 18, CoinType.Erc20("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"))


        )
    }

    override val derivationSettings: List<DerivationSetting>
        get() = listOf(
                DerivationSetting(CoinType.Bitcoin, AccountType.Derivation.bip49),
                DerivationSetting(CoinType.Litecoin, AccountType.Derivation.bip49)
        )

    override val syncModeSettings: List<SyncModeSetting>
        get() = listOf(
                SyncModeSetting(CoinType.Bitcoin, SyncMode.Fast),
                SyncModeSetting(CoinType.BitcoinCash, SyncMode.Fast),
                SyncModeSetting(CoinType.Litecoin, SyncMode.Fast),
                SyncModeSetting(CoinType.Dash, SyncMode.Fast)
        )

    override val communicationSettings: List<CommunicationSetting>
        get() = listOf(CommunicationSetting(CoinType.Ethereum, CommunicationMode.Infura))

    //  ILanguageConfigProvider

    override val localizations: List<String>
        get() {
            val coinsString = "de,en,es,fa,fr,ko,ru,tr,zh"
            return coinsString.split(",")
        }

    //  IAppConfigTestMode

    override val testMode: Boolean = BuildConfig.testMode

}
