package io.horizontalsystems.bankwallet.core.providers

import io.horizontalsystems.bankwallet.BuildConfig
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.entities.*
import io.horizontalsystems.core.IBuildConfigProvider
import io.horizontalsystems.core.ILanguageConfigProvider
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal

class AppConfigProvider : IAppConfigProvider, ILanguageConfigProvider, IBuildConfigProvider {

    override val companyWebPageLink: String = "https://horizontalsystems.io"
    override val appWebPageLink: String = "https://unstoppable.money"
    override val appGithubLink: String = "https://github.com/horizontalsystems/unstoppable-wallet-android"
    override val companyTwitterLink: String = "https://twitter.com/UnstoppableByHS"
    override val companyTelegramLink: String = "https://t.me/unstoppable_announcements"
    override val companyRedditLink: String = "https://reddit.com/r/UNSTOPPABLEWallet/"
    override val reportEmail = "unstoppable.support@protonmail.com"
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
            Currency(code = "USD", symbol = "\u0024", decimal = 2),
            Currency(code = "EUR", symbol = "\u20AC", decimal = 2),
            Currency(code = "GBP", symbol = "\u00A3", decimal = 2),
            Currency(code = "JPY", symbol = "\u00A5", decimal = 2)
    )

    override val featuredCoins: List<Coin> by lazy {
        listOf(
                Coin("BTC", "Bitcoin", "BTC", 8, CoinType.Bitcoin),
                Coin("LTC", "Litecoin", "LTC", 8, CoinType.Litecoin),
                Coin("ETH", "Ethereum", "ETH", 18, CoinType.Ethereum),
                Coin("BCH", "Bitcoin Cash", "BCH", 8, CoinType.BitcoinCash),
                Coin("DASH", "Dash", "DASH", 8, CoinType.Dash),
                Coin("BNB", "Binance Chain", "BNB", 8, CoinType.Binance("BNB")),
                Coin("ZEC", "Zcash", "ZEC", 8, CoinType.Zcash)
        )
    }

    override val ethereumCoin: Coin
        get() = featuredCoins[2]

    override val binanceCoin: Coin
        get() = featuredCoins[5]

    override val otherCoins: List<Coin> by lazy {
        listOf(
                Coin("ZRX",         "0x Protocol",          "ZRX",      18, CoinType.Erc20("0xE41d2489571d322189246DaFA5ebDe1F4699F498")),
                Coin("1INCH",       "1INCH Token",          "1INCH",    18, CoinType.Erc20("0x111111111117dc0aa78b770fa6a738034120c302")),
                Coin("LEND",        "Aave",                 "LEND",     18, CoinType.Erc20("0x80fB784B7eD66730e8b1DBd9820aFD29931aab03")),
                Coin("ADAI",        "Aave DAI",             "ADAI",     18, CoinType.Erc20("0xfC1E690f61EFd961294b3e1Ce3313fBD8aa4f85d")),
                Coin("AAVE",        "Aave Token",           "AAVE",     18, CoinType.Erc20("0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9")),
                Coin("ELF",         "Aelf",                 "ELF",      18, CoinType.Erc20("0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e")),
                Coin("AST",         "Air Swap",             "AST",      4, CoinType.Erc20("0x27054b13b1b798b345b591a4d22e6562d47ea75a")),
                Coin("AKRO",        "Akropolis",            "AKRO",     18, CoinType.Erc20("0x8ab7404063ec4dbcfd4598215992dc3f8ec853d7")),
                Coin("AMN",         "Amon",                 "AMN",      18, CoinType.Erc20("0x737f98ac8ca59f2c68ad658e3c3d8c8963e40a4c")),
                Coin("AMPL",        "Ampleforth",           "AMPL",     9, CoinType.Erc20("0xd46ba6d942050d489dbd938a2c909a5d5039a161")),
                Coin("ANKR",        "Ankr Network",         "ANKR",     8, CoinType.Binance("ANKR-E97")),
                Coin("ANT",         "Aragon",               "ANT",      18, CoinType.Erc20("0x960b236A07cf122663c4303350609A66A7B288C0")),
                Coin("ANJ",         "Aragon Court",         "ANJ",      18, CoinType.Erc20("0xcD62b1C403fa761BAadFC74C525ce2B51780b184")),
                Coin("REP",         "Augur",                "REP",      18, CoinType.Erc20("0x1985365e9f78359a9B6AD760e32412f4a445E862")),
                Coin("BAL",         "Balancer",             "BAL",      18, CoinType.Erc20("0xba100000625a3754423978a60c9317c58a424e3D")),
                Coin("BNT",         "Bancor",               "BNT",      18, CoinType.Erc20("0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C")),
                Coin("BAND",        "Band Protocol",        "BAND",     18, CoinType.Erc20("0xba11d00c5f74255f56a5e366f4f77f5a186d7f55")),
                Coin("BASE",        "Base Protocol",        "BASE",     9, CoinType.Erc20("0x07150e919b4de5fd6a63de1f9384828396f25fdc")),
                Coin("BAT",         "Basic Attention Token","BAT",      18, CoinType.Erc20("0x0D8775F648430679A709E98d2b0Cb6250d2887EF")),
                Coin("BID",         "Bidao",                "BID",      18, CoinType.Erc20("0x25e1474170c4c0aA64fa98123bdc8dB49D7802fa")),
                Coin("BNB-ERC20",   "Binance ERC20",        "BNB",      18, CoinType.Erc20("0xB8c77482e45F1F44dE1745F52C74426C631bDD52")),
                Coin("BUSD",        "Binance USD",          "BUSD",     8, CoinType.Binance("BUSD-BD1")),
                Coin("BTCB",        "Bitcoin BEP2",         "BTCB",     8, CoinType.Binance("BTCB-1DE")),
                Coin("BLT",         "Bloom",                "BLT",      18, CoinType.Erc20("0x107c4504cd79c5d2696ea0030a8dd4e92601b82e")),
                Coin("BZRX",        "bZx Protocol Token",   "BZRX",     18, CoinType.Erc20("0x56d811088235F11C8920698a204A5010a788f4b3")),
                Coin("CAS",         "Cashaaa",              "CAS",      8, CoinType.Binance("CAS-167")),
                Coin("CELR",        "Celer Network",        "CELR",     18, CoinType.Erc20("0x4f9254c83eb525f9fcf346490bbb3ed28a81c667")),
                Coin("CEL",         "Celsius",              "CEL",      4, CoinType.Erc20("0xaaaebe6fe48e54f431b0c390cfaf0b017d09d42d")),
                Coin("LINK",        "Chainlink",            "LINK",     18, CoinType.Erc20("0x514910771AF9Ca656af840dff83E8264EcF986CA")),
                Coin("CVC",         "Civic",                "CVC",      8, CoinType.Erc20("0x41e5560054824ea6b0732e656e3ad64e20e94e45")),
                Coin("CHAI",        "Chai",                 "CHAI",     18, CoinType.Erc20("0x06AF07097C9Eeb7fD685c692751D5C66dB49c215")),
                Coin("CHZ",         "Chiliz",               "CHZ",      8, CoinType.Binance("CHZ-ECD")),
                Coin("COMP",        "Compound",             "COMP",     18, CoinType.Erc20("0xc00e94cb662c3520282e6f5717214004a7f26888")),
                Coin("cDAI",        "Compound Dai",         "cDAI",     8, CoinType.Erc20("0x5d3a536E4D6DbD6114cc1Ead35777bAB948E3643")),
                Coin("cSAI",        "Compound Sai",         "cSAI",     8, CoinType.Erc20("0xF5DCe57282A584D2746FaF1593d3121Fcac444dC")),
                Coin("cUSDC",       "Compound USD Coin",    "cUSDC",    8, CoinType.Erc20("0x39AA39c021dfbaE8faC545936693aC917d5E7563")),
                Coin("COS",         "Contentos",            "COS",      8, CoinType.Binance("COS-2E4")),
                Coin("CRO",         "Crypto.com Coin",      "CRO",      8, CoinType.Erc20("0xA0b73E1Ff0B80914AB6fe0444E65848C4C34450b")),
                Coin("XCHF",        "CryptoFranc",          "XCHF",     18, CoinType.Erc20("0xB4272071eCAdd69d933AdcD19cA99fe80664fc08")),
                Coin("CRPT",        "Crypterium",           "CRPT",     8, CoinType.Binance("CRPT-8C9")),
                Coin("CRV",         "Curve DAO Token",      "CRV",      18, CoinType.Erc20("0xD533a949740bb3306d119CC777fa900bA034cd52")),
                Coin("DAI",         "Dai",                  "DAI",      18, CoinType.Erc20("0x6b175474e89094c44da98b954eedeac495271d0f")),
                Coin("GEN",         "DAOstack",             "GEN",      18, CoinType.Erc20("0x543ff227f64aa17ea132bf9886cab5db55dcaddf")),
                Coin("RING",        "Darwinia Network",     "RING",    18, CoinType.Erc20("0x9469d013805bffb7d3debe5e7839237e535ec483")),
                Coin("MANA",        "Decentraland",         "MANA",     18, CoinType.Erc20("0x0F5D2fB29fb7d3CFeE444a200298f468908cC942")),
                Coin("USDx",        "dForce",               "USDx",     18, CoinType.Erc20("0xeb269732ab75A6fD61Ea60b06fE994cD32a83549")),
                Coin("DIA",         "Dia",                  "DIA",      18, CoinType.Erc20("0x84ca8bc7997272c7cfb4d0cd3d55cd942b3c9419")),
                Coin("DGD",         "DigixDAO",             "DGD",      9, CoinType.Erc20("0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A")),
                Coin("DGX",         "Digix Gold Token",     "DGX",      9, CoinType.Erc20("0x4f3AfEC4E5a3F2A6a1A411DEF7D7dFe50eE057bF", minimumSendAmount = BigDecimal("0.001"))),
                Coin("DNT",         "District0x",           "DNT",      18, CoinType.Erc20("0x0abdace70d3790235af448c88547603b945604ea")),
                Coin("DHT",         "dHedge DAO Token",     "DHT",      18, CoinType.Erc20("0xca1207647Ff814039530D7d35df0e1Dd2e91Fa84")),
                Coin("DOS",         "DOSNetwork",           "DOS",      8, CoinType.Binance("DOS-120")),
                Coin("DOS-ERC20",   "DOSNetwork",           "DOS",      18, CoinType.Erc20("0x0A913beaD80F321E7Ac35285Ee10d9d922659cB7")),
                Coin("ENJ",         "EnjinCoin",            "ENJ",      18, CoinType.Erc20("0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c")),
                Coin("ETH-BEP2",    "ETH BEP2",             "ETH",      8, CoinType.Binance("ETH-1C9")),
                Coin("FLASH",       "Flash Token",          "FLASH",    18, CoinType.Erc20("0xb4467e8d621105312a914f1d42f10770c0ffe3c8")),
                Coin("FOAM",        "FOAM Token",           "FOAM",     18, CoinType.Erc20("0x4946fcea7c692606e8908002e55a582af44ac121")),
                Coin("FUN",         "FunFair",              "FUN",      8, CoinType.Erc20("0x419d0d8bdd9af5e606ae2232ed285aff190e711b")),
                Coin("FYZ",         "FYOOZ",                "FYZ",      18, CoinType.Erc20("0x6BFf2fE249601ed0Db3a87424a2E923118BB0312")),
                Coin("GUSD",        "Gemini Dollar",        "GUSD",     2, CoinType.Erc20("0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd")),
                Coin("GNO",         "Gnosis",               "GNO",      18, CoinType.Erc20("0x6810e776880c02933d47db1b9fc05908e5386b96")),
                Coin("GRT",         "Graph Token",          "GRT",      18, CoinType.Erc20("0xc944e90c64b2c07662a292be6244bdf05cda44a7")),
                Coin("GRID",        "Grid",                 "GRID",     12, CoinType.Erc20("0x12b19d3e2ccc14da04fae33e63652ce469b3f2fd")),
                Coin("GLM",         "Golem",                "GLM",      18, CoinType.Erc20("0x7DD9c5Cba05E151C895FDe1CF355C9A1D5DA6429")),
                Coin("GTO",         "Gifto",                "GTO",      8, CoinType.Binance("GTO-908")),
                Coin("ONE",         "Harmony",              "ONE",      8, CoinType.Binance("ONE-5F9")),
                Coin("HEDG",        "HEDG Trade",           "HEDG",     18, CoinType.Erc20("0xF1290473E210b2108A85237fbCd7b6eb42Cc654F")),
                Coin("HOT",         "Holo",                 "HOT",      18, CoinType.Erc20("0x6c6EE5e31d828De241282B9606C8e98Ea48526E2")),
                Coin("HT",          "Huobi Token",          "HT",       18, CoinType.Erc20("0x6f259637dcD74C767781E37Bc6133cd6A68aa161")),
                Coin("IDEX",        "IDEX",                 "IDEX",     18, CoinType.Erc20("0xB705268213D593B8FD88d3FDEFF93AFF5CbDcfAE")),
                Coin("IRIS",        "IRISnet",              "IRIS",     8, CoinType.Binance("IRIS-D88")),
                Coin("KEY",         "KEY",                  "KEY",      18, CoinType.Erc20("0x4Cd988AfBad37289BAAf53C13e98E2BD46aAEa8c")),
                Coin("KCS",         "KuCoin Shares",        "KCS",      6, CoinType.Erc20("0x039B5649A59967e3e936D7471f9c3700100Ee1ab", minimumRequiredBalance = BigDecimal("0.001"))),
                Coin("KNC",         "Kyber Network Crystal","KNC",      18, CoinType.Erc20("0xdd974D5C2e2928deA5F71b9825b8b646686BD200")),
                Coin("LQD",         "Liquidity.Network Token", "LQD",   18, CoinType.Erc20("0xD29F0b5b3F50b07Fe9a9511F7d86F4f4bAc3f8c4")),
                Coin("LINA",        "Linear Token",           "LINA",   18, CoinType.Erc20("0x3E9BC21C9b189C09dF3eF1B824798658d5011937")),
                Coin("LOOM",        "Loom Network",         "LOOM",     18, CoinType.Erc20("0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0")),
                Coin("LRC",         "Loopring",             "LRC",      18, CoinType.Erc20("0xEF68e7C694F40c8202821eDF525dE3782458639f")),
                Coin("LTO",         "LTO Network",          "LTO",      8, CoinType.Binance("LTO-BDF")),
                Coin("MKR",         "Maker",                "MKR",      18, CoinType.Erc20("0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2")),
                Coin("MATIC-BEP2",  "Matic Token",          "MATIC",    8, CoinType.Binance("MATIC-84A")),
                Coin("MCO",         "MCO",                  "MCO",      8, CoinType.Erc20("0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d")),
                Coin("TKN",         "Monolith",             "TKN",      8, CoinType.Erc20("0xaaaf91d9b90df800df4f55c205fd6989c977e73a")),
                Coin("NEXO",        "Nexo",                 "NEXO",     18, CoinType.Erc20("0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206")),
                Coin("XFT",         "Offshift",             "XFT",      18, CoinType.Erc20("0xabe580e7ee158da464b51ee1a83ac0289622e6be")),
                Coin("OMG",         "OmiseGO",              "OMG",      18, CoinType.Erc20("0xd26114cd6EE289AccF82350c8d8487fedB8A0C07")),
                Coin("ORBS",        "Orbs",                 "ORBS",     18, CoinType.Erc20("0xff56Cc6b1E6dEd347aA0B7676C85AB0B3D08B0FA")),
                Coin("OXT",         "Orchid",               "OXT",      18, CoinType.Erc20("0x4575f41308EC1483f3d399aa9a2826d74Da13Deb")),
                Coin("ORN",         "Orion Protocol",       "ORN",      8, CoinType.Erc20("0x0258F474786DdFd37ABCE6df6BBb1Dd5dfC4434a")),
                Coin("PAN",         "Panvala pan",          "PAN",      18, CoinType.Erc20("0xD56daC73A4d6766464b38ec6D91eB45Ce7457c44")),
                Coin("PAR",         "Parachute",            "PAR",      18, CoinType.Erc20("0x1beef31946fbbb40b877a72e4ae04a8d1a5cee06")),
                Coin("PAX",         "Paxos Standard",       "PAX",      18, CoinType.Erc20("0x8E870D67F660D95d5be530380D0eC0bd388289E1")),
                Coin("POLS",        "PolkastarterToken",    "POLS",     18, CoinType.Erc20("0x83e6f1E41cdd28eAcEB20Cb649155049Fac3D5Aa")),
                Coin("POLY",        "Polymath",             "POLY",     18, CoinType.Erc20("0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC")),
                Coin("PPT",         "Populous",             "PPT",      8, CoinType.Erc20("0xd4fa1460F537bb9085d22C7bcCB5DD450Ef28e3a")),
                Coin("NPXS",        "Pundi X",              "NPXS",     18, CoinType.Erc20("0xA15C7Ebe1f07CaF6bFF097D8a589fb8AC49Ae5B3")),
                Coin("RARI",        "Rarible",              "RARI",     18, CoinType.Erc20("0xfca59cd816ab1ead66534d82bc21e7515ce441cf")) ,
                Coin("RFI",         "reflect.finance",      "RFI",      9, CoinType.Erc20("0xA1AFFfE3F4D611d252010E3EAf6f4D77088b0cd7")),
                Coin("REN",         "Ren",                  "REN",      18, CoinType.Erc20("0x408e41876cccdc0f92210600ef50372656052a38")),
                Coin("RENBCH",      "renBCH",               "RENBCH",   8, CoinType.Erc20("0x459086f2376525bdceba5bdda135e4e9d3fef5bf")),
                Coin("RENBTC",      "renBTC",               "RENBTC",   8, CoinType.Erc20("0xeb4c2781e4eba804ce9a9803c67d0893436bb27d")),
                Coin("RENZEC",      "renZEC",               "RENZEC",   8, CoinType.Erc20("0x1c5db575e2ff833e46a2e9864c22f4b22e0b37c2")),
                Coin("Rev",         "Revain",               "REV",      6, CoinType.Erc20("0x2ef52Ed7De8c5ce03a4eF0efbe9B7450F2D7Edc9")),
                Coin("XRP",         "Ripple BEP2",          "XRP",      8, CoinType.Binance("XRP-BF2")),
                Coin("RUNE",        "THORchain",            "RUNE",     8, CoinType.Binance("RUNE-B1A")),
                Coin("EURS",        "STASIS EURO",          "EURS",     2, CoinType.Erc20("0xdB25f211AB05b1c97D595516F45794528a807ad8")),
                Coin("SAI",         "Sai",                  "SAI",      18, CoinType.Erc20("0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359")),
                Coin("SHR",         "ShareToken",           "SHR",      8, CoinType.Binance("SHR-DB6")),
                Coin("XOR",         "Sora",                 "XOR",      18, CoinType.Erc20("0x40FD72257597aA14C7231A7B1aaa29Fce868F677")),
                Coin("SNT",         "Status",               "SNT",      18, CoinType.Erc20("0x744d70FDBE2Ba4CF95131626614a1763DF805B9E")),
                Coin("STAKE",       "xDai",                 "STAKE",    18, CoinType.Erc20("0x0Ae055097C6d159879521C384F1D2123D1f195e6")),
                Coin("CHSB",        "SwissBorg",            "CHSB",     8, CoinType.Erc20("0xba9d4199fab4f26efe3551d490e3821486f135ba")),
                Coin("SXP",         "Swipe",                "SXP",      18, CoinType.Erc20("0x8ce9137d39326ad0cd6491fb5cc0cba0e089b6a9")),
                Coin("SNX",         "Synthetix",            "SNX",      18, CoinType.Erc20("0xC011a73ee8576Fb46F5E1c5751cA3B9Fe0af2a6F")),
                Coin("sETH",        "Synt sETH",            "sETH",     18, CoinType.Erc20("0x5e74C9036fb86BD7eCdcb084a0673EFc32eA31cb")),
                Coin("sUSD",        "Synth sUSD",           "sUSD",     18, CoinType.Erc20("0x57Ab1ec28D129707052df4dF418D58a2D46d5f51")),
                Coin("sXAU",        "Synt sXAU",            "sXAU",     18, CoinType.Erc20("0x261EfCdD24CeA98652B9700800a13DfBca4103fF")),
                Coin("USDT",        "Tether USD",           "USDT",     6, CoinType.Erc20("0xdAC17F958D2ee523a2206206994597C13D831ec7")),
                Coin("MTXLT",       "Tixl",                 "MTXLT",    8, CoinType.Binance("MTXLT-286")),
                Coin("TAUDB",       "TrueAUD",              "TAUDB",    8, CoinType.Binance("TAUDB-888")),
                Coin("THKDB",       "TrueHKD",              "THKDB",    8, CoinType.Binance("THKDB-888")),
                Coin("TUSDB",       "TrueUSD",              "TUSDB",    8, CoinType.Binance("TUSDB-888")),
                Coin("TUSD",        "TrueUSD",              "TUSD",     18, CoinType.Erc20("0x0000000000085d4780B73119b644AE5ecd22b376")),
                Coin("SWAP",        "TrustSwap",            "SWAP",     18, CoinType.Erc20("0xCC4304A31d09258b0029eA7FE63d032f52e44EFe")),
                Coin("UNI",         "Uniswap",              "UNI",      18, CoinType.Erc20("0x1f9840a85d5af5bf1d1762f925bdaddc4201f984")),
                Coin("USDC",        "USD Coin",             "USDC",     6, CoinType.Erc20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")),
                Coin("UTK",         "Utrust",               "UTK",      18, CoinType.Erc20("0xdc9Ac3C20D1ed0B540dF9b1feDC10039Df13F99c")),
                Coin("WICC",        "WaykiChain Coin",      "WICC",     8, CoinType.Binance("WICC-01D")),
                Coin("WTC",         "Waltonchain",          "WTC",      18, CoinType.Erc20("0xb7cB1C96dB6B22b0D3d9536E0108d062BD488F74")),
                Coin("WRX",         "WazirX Token",         "WRX",      8, CoinType.Binance("WRX-ED1")),
                Coin("WBTC",        "Wrapped Bitcoin",      "WBTC",     8, CoinType.Erc20("0x2260fac5e5542a773aa44fbcfedf7c193bc2c599")),
                Coin("WETH",        "Wrapped Ethereum",     "WETH",     18, CoinType.Erc20("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2")),
                Coin("WFIL",        "Wrapped Filecoin",     "WFIL",     18, CoinType.Erc20("0x6e1A19F235bE7ED8E3369eF73b196C07257494DE")),
                Coin("WZEC",        "Wrapped ZEC",          "WZEC",     18, CoinType.Erc20("0x4a64515e5e1d1073e83f30cb97bed20400b66e10")),
                Coin("XIO",         "XIO Network",          "XIO",      18, CoinType.Erc20("0x0f7F961648aE6Db43C75663aC7E5414Eb79b5704")),
                )
    }

    //  ILanguageConfigProvider

    override val localizations: List<String>
        get() {
            val coinsString = "de,en,es,fa,fr,ko,ru,tr,zh"
            return coinsString.split(",")
        }

    //  IBuildConfigProvider

    override val testMode: Boolean = BuildConfig.testMode

    override val skipRootCheck: Boolean = BuildConfig.skipRootCheck

}
