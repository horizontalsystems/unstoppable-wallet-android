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
    override val btcCoreRpcUrl: String = "https://btc.horizontalsystems.xyz/rpc"

    override val cryptoCompareApiKey = App.instance.getString(R.string.cryptoCompareApiKey)
    override val infuraProjectId = App.instance.getString(R.string.infuraProjectId)
    override val infuraProjectSecret = App.instance.getString(R.string.infuraSecretKey)
    override val etherscanApiKey = App.instance.getString(R.string.etherscanKey)
    override val bscscanApiKey = App.instance.getString(R.string.bscscanKey)
    override val guidesUrl = App.instance.getString(R.string.guidesUrl)
    override val faqUrl = App.instance.getString(R.string.faqUrl)
    override val fiatDecimal: Int = 2
    override val maxDecimal: Int = 8
    override val feeRateAdjustForCurrencies: List<String> = listOf("USD","EUR")

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
                Coin("BNB-BSC", "Binance Smart Chain", "BNB", 18, CoinType.BinanceSmartChain),
                Coin("BNB", "Binance Chain", "BNB", 8, CoinType.Binance("BNB")),
                Coin("ZEC", "Zcash", "ZEC", 8, CoinType.Zcash)
        )
    }

    override val ethereumCoin: Coin
        get() = featuredCoins[2]

    override val binanceSmartChainCoin: Coin
        get() = featuredCoins[5]

    override val binanceCoin: Coin
        get() = featuredCoins[6]

    override val otherCoins: List<Coin> by lazy {
        listOf(
                Coin("\$BASED",     "\$BASED",              "\$BASED",  18, CoinType.Erc20("0x68A118Ef45063051Eac49c7e647CE5Ace48a68a5")),
                Coin("ZCN",         "0chain",               "ZCN",      10, CoinType.Erc20("0xb9EF770B6A5e12E45983C5D80545258aA38F3B78")),
                Coin("ZRX",         "0x Protocol",          "ZRX",      18, CoinType.Erc20("0xE41d2489571d322189246DaFA5ebDe1F4699F498")),
                Coin("1INCH",       "1INCH Token",          "1INCH",    18, CoinType.Erc20("0x111111111117dc0aa78b770fa6a738034120c302")),
                Coin("MPH",         "88mph.app",            "MPH",      18, CoinType.Erc20("0x8888801af4d980682e47f1a9036e589479e835c5")),
                Coin("LEND",        "Aave",                 "LEND",     18, CoinType.Erc20("0x80fB784B7eD66730e8b1DBd9820aFD29931aab03")),
                Coin("AAVE",        "Aave Token",           "AAVE",     18, CoinType.Erc20("0x7fc66500c84a76ad7e9c93437bfc5ac33e2ddae9")),
                Coin("AAVEDAI",     "Aave DAI",             "ADAI",     18, CoinType.Erc20("0xfC1E690f61EFd961294b3e1Ce3313fBD8aa4f85d")),
                Coin("ELF",         "Aelf",                 "ELF",      18, CoinType.Erc20("0xbf2179859fc6D5BEE9Bf9158632Dc51678a4100e")),
                Coin("AST",         "AirSwap",              "AST",      4,  CoinType.Erc20("0x27054b13b1b798b345b591a4d22e6562d47ea75a")),
                Coin("AKRO",        "Akropolis",            "AKRO",     18, CoinType.Erc20("0x8ab7404063ec4dbcfd4598215992dc3f8ec853d7")),
                Coin("ALBT",        "AllianceBlock Token",  "ALBT",     18, CoinType.Erc20("0x00a8b738E453fFd858a7edf03bcCfe20412f0Eb0")),
                Coin("ALPHA",       "AlphaToken",           "ALPHA",    18, CoinType.Erc20("0xa1faa113cbe53436df28ff0aee54275c13b40975")),
                Coin("AMON",        "Amon",                 "AMN",      18, CoinType.Erc20("0x737f98ac8ca59f2c68ad658e3c3d8c8963e40a4c")),
                Coin("AMPL",        "Ampleforth",           "AMPL",     9,  CoinType.Erc20("0xd46ba6d942050d489dbd938a2c909a5d5039a161")),
                Coin("ANKR",        "Ankr Network",         "ANKR",     8,  CoinType.Binance("ANKR-E97")),
                Coin("API3",        "API3",                 "API3",     18, CoinType.Erc20("0x0b38210ea11411557c13457D4dA7dC6ea731B88a")),
                Coin("APY",         "APY Governance Token", "APY",      18, CoinType.Erc20("0x95a4492F028aa1fd432Ea71146b433E7B4446611")),
                Coin("ANT",         "Aragon",               "ANT",      18, CoinType.Erc20("0x960b236A07cf122663c4303350609A66A7B288C0")),
                Coin("ANJ",         "Aragon Court",         "ANJ",      18, CoinType.Erc20("0xcD62b1C403fa761BAadFC74C525ce2B51780b184")),
                Coin("AUC",         "Auctus",               "AUC",      18, CoinType.Erc20("0xc12d099be31567add4e4e4d0d45691c3f58f5663")),
                Coin("REP",         "Augur",                "REP",      18, CoinType.Erc20("0x1985365e9f78359a9B6AD760e32412f4a445E862")),
                Coin("BAC",         "BAC",                  "BAC",      18, CoinType.Erc20("0x3449FC1Cd036255BA1EB19d65fF4BA2b8903A69a")),
                Coin("BADGER",      "Badger",               "BADGER",   18, CoinType.Erc20("0x3472a5a71965499acd81997a54bba8d852c6e53d")),
                Coin("BAL",         "Balancer",             "BAL",      18, CoinType.Erc20("0xba100000625a3754423978a60c9317c58a424e3D")),
                Coin("BNT",         "Bancor",               "BNT",      18, CoinType.Erc20("0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C")),
                Coin("BAND",        "Band Protocol",        "BAND",     18, CoinType.Erc20("0xba11d00c5f74255f56a5e366f4f77f5a186d7f55")),
                Coin("BOND",        "BarnBridge",           "BOND",     18, CoinType.Erc20("0x0391D2021f89DC339F60Fff84546EA23E337750f")),
                Coin("BASE",        "Base Protocol",        "BASE",     9,  CoinType.Erc20("0x07150e919b4de5fd6a63de1f9384828396f25fdc")),
                Coin("BAT",         "Basic Attention Token","BAT",      18, CoinType.Erc20("0x0D8775F648430679A709E98d2b0Cb6250d2887EF")),
                Coin("BID",         "Bidao",                "BID",      18, CoinType.Erc20("0x25e1474170c4c0aA64fa98123bdc8dB49D7802fa")),
                Coin("BNB-ERC20",   "Binance ERC20",        "BNB",      18, CoinType.Erc20("0xB8c77482e45F1F44dE1745F52C74426C631bDD52")),
                Coin("BUSD",        "Binance USD",          "BUSD",     8,  CoinType.Binance("BUSD-BD1")),
                Coin("BTCB",        "Bitcoin BEP2",         "BTCB",     8,  CoinType.Binance("BTCB-1DE")),
                Coin("BLT",         "Bloom",                "BLT",      18, CoinType.Erc20("0x107c4504cd79c5d2696ea0030a8dd4e92601b82e")),
                Coin("BONDLY",      "Bondly Token",         "BONDLY",   18, CoinType.Erc20("0xd2dda223b2617cb616c1580db421e4cfae6a8a85")),
                Coin("BZRX",        "bZx Protocol Token",   "BZRX",     18, CoinType.Erc20("0x56d811088235F11C8920698a204A5010a788f4b3")),
                Coin("CAS",         "Cashaa",               "CAS",      8,  CoinType.Binance("CAS-167")),
                Coin("CELR",        "Celer Network",        "CELR",     18, CoinType.Erc20("0x4f9254c83eb525f9fcf346490bbb3ed28a81c667")),
                Coin("CEL",         "Celsius",              "CEL",      4,  CoinType.Erc20("0xaaaebe6fe48e54f431b0c390cfaf0b017d09d42d")),
                Coin("CHAI",        "Chai",                 "CHAI",     18, CoinType.Erc20("0x06AF07097C9Eeb7fD685c692751D5C66dB49c215")),
                Coin("CHAIN",       "Chain Games",          "CHAIN",    18, CoinType.Erc20("0xC4C2614E694cF534D407Ee49F8E44D125E4681c4")),
                Coin("LINK",        "Chainlink",            "LINK",     18, CoinType.Erc20("0x514910771AF9Ca656af840dff83E8264EcF986CA")),
                Coin("CHZ",         "Chiliz",               "CHZ",      8,  CoinType.Binance("CHZ-ECD")),
                Coin("CVC",         "Civic",                "CVC",      8,  CoinType.Erc20("0x41e5560054824ea6b0732e656e3ad64e20e94e45")),
                Coin("COMP",        "Compound",             "COMP",     18, CoinType.Erc20("0xc00e94cb662c3520282e6f5717214004a7f26888")),
                Coin("CDAI",        "Compound Dai",         "CDAI",     8,  CoinType.Erc20("0x5d3a536E4D6DbD6114cc1Ead35777bAB948E3643")),
                Coin("CSAI",        "Compound Sai",         "CSAI",     8,  CoinType.Erc20("0xf5dce57282a584d2746faf1593d3121fcac444dc")),
                Coin("CUSDC",       "Compound USDC",        "CUSDC",    8,  CoinType.Erc20("0x39aa39c021dfbae8fac545936693ac917d5e7563")),
                Coin("COS",         "Contentos",            "COS",      8,  CoinType.Binance("COS-2E4")),
                Coin("CREAM",       "Cream",                "CREAM",    18, CoinType.Erc20("0x2ba592f78db6436527729929aaf6c908497cb200")),
                Coin("CRPT",        "Crypterium",           "CRPT",     8,  CoinType.Binance("CRPT-8C9")),
                Coin("CRO",         "Crypto.com Coin",      "CRO",      8,  CoinType.Erc20("0xA0b73E1Ff0B80914AB6fe0444E65848C4C34450b")),
                Coin("CC10",        "Cryptocurrency Top 10 Tokens Index",  "CC10",      18, CoinType.Erc20("0x17ac188e09a7890a1844e5e65471fe8b0ccfadf3")),
                Coin("CRV",         "Curve DAO Token",      "CRV",      18, CoinType.Erc20("0xD533a949740bb3306d119CC777fa900bA034cd52")),
                Coin("CORE",        "cVault.finance",       "CORE",     18, CoinType.Erc20("0x62359ed7505efc61ff1d56fef82158ccaffa23d7")),
                Coin("DAI",         "Dai",                  "DAI",      18, CoinType.Erc20("0x6b175474e89094c44da98b954eedeac495271d0f")),
                Coin("RING",        "Darwinia Network",     "RING",     18, CoinType.Erc20("0x9469d013805bffb7d3debe5e7839237e535ec483")),
                Coin("GEN",         "DAOstack",             "GEN",      18, CoinType.Erc20("0x543ff227f64aa17ea132bf9886cab5db55dcaddf")),
                Coin("MANA",        "Decentraland",         "MANA",     18, CoinType.Erc20("0x0F5D2fB29fb7d3CFeE444a200298f468908cC942")),
                Coin("DPI",         "DefiPulse Index",      "DPI",      18, CoinType.Erc20("0x1494ca1f11d487c2bbe4543e90080aeba4ba3c2b")),
                Coin("DYP",         "DeFiYieldProtocol",    "DYP",      18, CoinType.Erc20("0x961C8c0B1aaD0c0b10a51FeF6a867E3091BCef17")),
                Coin("DEFO",        "DefHold",              "DEFO",     18, CoinType.Erc20("0xe481f2311C774564D517d015e678c2736A25Ddd3")),
                Coin("DEFI5",       "DEFI Top 5 Tokens Index",  "DEFI5",      18, CoinType.Erc20("0xfa6de2697d59e88ed7fc4dfe5a33dac43565ea41")),
                Coin("DEGO",        "dego.finance",         "DEGO",     18, CoinType.Erc20("0x88ef27e69108b2633f8e1c184cc37940a075cc02")),
                Coin("DEUS",        "DEUS",                 "DEUS",     18, CoinType.Erc20("0x3b62F3820e0B035cc4aD602dECe6d796BC325325")),
                Coin("USDx",        "dForce",               "USDx",     18, CoinType.Erc20("0xeb269732ab75A6fD61Ea60b06fE994cD32a83549")),
                Coin("DHT",         "dHedge DAO Token",     "DHT",      18, CoinType.Erc20("0xca1207647Ff814039530D7d35df0e1Dd2e91Fa84")),
                Coin("DUSD",        "DefiDollar",           "DUSD",     18, CoinType.Erc20("0x5bc25f649fc4e26069ddf4cf4010f9f706c23831")),
                Coin("DIA",         "DIA",                  "DIA",      18, CoinType.Erc20("0x84ca8bc7997272c7cfb4d0cd3d55cd942b3c9419")),
                Coin("DGD",         "DigixDAO",             "DGD",      9,  CoinType.Erc20("0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A")),
                Coin("DGX",         "Digix Gold Token",     "DGX",      9,  CoinType.Erc20("0x4f3AfEC4E5a3F2A6a1A411DEF7D7dFe50eE057bF")),
                Coin("DNT",         "District0x",           "DNT",      18, CoinType.Erc20("0x0abdace70d3790235af448c88547603b945604ea")),
                Coin("DMG",         "DMM:Governance",       "DMG",      18, CoinType.Erc20("0xEd91879919B71bB6905f23af0A68d231EcF87b14")),
                Coin("DOS",         "DOS Network",          "DOS",      8,  CoinType.Binance("DOS-120")),
                Coin("DOS-ERC20",   "DOS Network",          "DOS",      18, CoinType.Erc20("0x0A913beaD80F321E7Ac35285Ee10d9d922659cB7")),
                Coin("DDIM",        "DuckDaoDime",          "DDIM",     18, CoinType.Erc20("0xfbeea1c75e4c4465cb2fccc9c6d6afe984558e20")),
                Coin("DSD",         "Dynamic Set Dollar",   "DSD",      18, CoinType.Erc20("0xbd2f0cd039e0bfcf88901c98c0bfac5ab27566e3")),
                Coin("eXRD",        "E-RADIX",              "eXRD",     18, CoinType.Erc20("0x6468e79A80C0eaB0F9A2B574c8d5bC374Af59414")),
                Coin("ENJ",         "Enjin Coin",           "ENJ",      18, CoinType.Erc20("0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c")),
                Coin("ESD",         "Empty Set Dollar",     "ESD",      18, CoinType.Erc20("0x36f3fd68e7325a35eb768f1aedaae9ea0689d723")),
                Coin("ETH-BEP2",    "ETH BEP2",             "ETH",      8,  CoinType.Binance("ETH-1C9")),
                Coin("DIP",         "Etherisc DIP Token",   "DIP",      18, CoinType.Erc20("0xc719d010b63e5bbf2c0551872cd5316ed26acd83")),
                Coin("ETHYS",       "Ethereum Stake",       "ETHYS",    18, CoinType.Erc20("0xD0d3EbCAd6A20ce69BC3Bc0e1ec964075425e533")),
                Coin("FSW",         "FalconSwap Token",     "FSW",      18, CoinType.Erc20("0xfffffffFf15AbF397dA76f1dcc1A1604F45126DB")),
                Coin("FARM",        "FARM Reward Token",    "FARM",     18, CoinType.Erc20("0xa0246c9032bC3A600820415aE600c6388619A14D")),
                Coin("FNK",         "Finiko",               "FNK",      18, CoinType.Erc20("0xb5fe099475d3030dde498c3bb6f3854f762a48ad")),
                Coin("FLASH",       "Flash Token",          "FLASH",    18, CoinType.Erc20("0xb4467e8d621105312a914f1d42f10770c0ffe3c8")),
                Coin("FLUX",        "FLUX",                 "FLUX",     18, CoinType.Erc20("0x469eDA64aEd3A3Ad6f868c44564291aA415cB1d9")),
                Coin("FOAM",        "FOAM Token",           "FOAM",     18, CoinType.Erc20("0x4946fcea7c692606e8908002e55a582af44ac121")),
                Coin("FRAX",        "Frax",                 "FRAX",     18, CoinType.Erc20("0x853d955acef822db058eb8505911ed77f175b99e")),
                Coin("FUN",         "FunFair",              "FUN",      8,  CoinType.Erc20("0x419d0d8bdd9af5e606ae2232ed285aff190e711b")),
                Coin("COMBO",       "Furucombo",            "COMBO",    18, CoinType.Erc20("0xffffffff2ba8f66d4e51811c5190992176930278")),
                Coin("FYZ",         "FYOOZ",                "FYZ",      18, CoinType.Erc20("0x6BFf2fE249601ed0Db3a87424a2E923118BB0312")),
                Coin("GST2",        "Gas Token Two",        "GST2",     2,  CoinType.Erc20("0x0000000000b3f879cb30fe243b4dfee438691c04")),
                Coin("GUSD",        "Gemini Dollar",        "GUSD",     2,  CoinType.Erc20("0x056Fd409E1d7A124BD7017459dFEa2F387b6d5Cd")),
                Coin("GTO",         "Gifto",                "GTO",      8,  CoinType.Binance("GTO-908")),
                Coin("GNO",         "Gnosis",               "GNO",      18, CoinType.Erc20("0x6810e776880c02933d47db1b9fc05908e5386b96")),
                Coin("GLM",         "Golem",                "GLM",      18, CoinType.Erc20("0x7DD9c5Cba05E151C895FDe1CF355C9A1D5DA6429")),
                Coin("GRT",         "Graph Token",          "GRT",      18, CoinType.Erc20("0xc944e90c64b2c07662a292be6244bdf05cda44a7")),
                Coin("GRID",        "Grid",                 "GRID",     12, CoinType.Erc20("0x12b19d3e2ccc14da04fae33e63652ce469b3f2fd")),
                Coin("XCHF",        "GryptoFranc",          "XCHF",     18, CoinType.Erc20("0xb4272071ecadd69d933adcd19ca99fe80664fc08")),
                Coin("ONE",         "Harmony",              "ONE",      8,  CoinType.Binance("ONE-5F9")),
                Coin("HEGIC",       "Hegic",                "HEGIC",    18, CoinType.Erc20("0x584bC13c7D411c00c01A62e8019472dE68768430")),
                Coin("HEDG",        "HEDG",                 "HEDG",     18, CoinType.Erc20("0xf1290473e210b2108a85237fbcd7b6eb42cc654f")),
                Coin("HEZ",         "Hermez Network Token", "HEZ",      18, CoinType.Erc20("0xEEF9f339514298C6A857EfCfC1A762aF84438dEE")),
                Coin("HLAND",       "Hland Token",          "HLAND",    18, CoinType.Erc20("0xba7b2c094c1a4757f9534a37d296a3bed7f544dc")),
                Coin("HOT",         "Holo",                 "HOT",      18, CoinType.Erc20("0x6c6EE5e31d828De241282B9606C8e98Ea48526E2")),
                Coin("HH",          "Holyheld",             "HH",       18, CoinType.Erc20("0x3FA729B4548beCBAd4EaB6EF18413470e6D5324C")),
                Coin("HT",          "Huobi Token",          "HT",       18, CoinType.Erc20("0x6f259637dcD74C767781E37Bc6133cd6A68aa161")),
                Coin("HUSD",        "HUSD",                 "HUSD",     8,  CoinType.Erc20("0xdf574c24545e5ffecb9a659c229253d4111d87e1")),
                Coin("IDEX",        "IDEX",                 "IDEX",     18, CoinType.Erc20("0xB705268213D593B8FD88d3FDEFF93AFF5CbDcfAE")),
                Coin("IDLE",        "Idle",                 "IDLE",     18, CoinType.Erc20("0x875773784Af8135eA0ef43b5a374AaD105c5D39e")),
                Coin("IOTX",        "IoTeX",                "IOTX",     18, CoinType.Erc20("0x6fb3e0a217407efff7ca062d46c26e5d60a14d69")),
                Coin("IRIS",        "IRISnet",              "IRIS",     8,  CoinType.Binance("IRIS-D88")),
                Coin("KEEP",        "KEEP Token",           "KEEP",     18, CoinType.Erc20("0x85eee30c52b0b379b046fb0f85f4f3dc3009afec")),
                Coin("KP3R",        "Keep3rV1",             "KP3R",     18, CoinType.Erc20("0x1ceb5cb57c4d4e2b2433641b95dd330a33185a44")),
                Coin("PNK",         "Kleros",               "PNK",      18, CoinType.Erc20("0x93ed3fbe21207ec2e8f2d3c3de6e058cb73bc04d")),
                Coin("KCS",         "KuCoin Shares",        "KCS",      6,  CoinType.Erc20("0x039B5649A59967e3e936D7471f9c3700100Ee1ab")),
                Coin("KNC",         "Kyber Network Crystal","KNC",      18, CoinType.Erc20("0xdd974D5C2e2928deA5F71b9825b8b646686BD200")),
                Coin("LGCY",        "LGCY Network",         "LGCY",     18, CoinType.Erc20("0xaE697F994Fc5eBC000F8e22EbFfeE04612f98A0d")),
                Coin("LDO",         "Lido DAO Token",       "LDO",      18, CoinType.Erc20("0x5a98fcbea516cf06857215779fd812ca3bef1b32")),
                Coin("LINA",        "Linear Token",         "LINA",     18, CoinType.Erc20("0x3E9BC21C9b189C09dF3eF1B824798658d5011937")),
                Coin("LPT",         "Livepeer Token",       "LPT",      18, CoinType.Erc20("0x58b6a8a3302369daec383334672404ee733ab239")),
                Coin("LQD",         "Liquidity Network",    "LQD",      18, CoinType.Erc20("0xd29f0b5b3f50b07fe9a9511f7d86f4f4bac3f8c4")),
                Coin("LON",         "LON Token",            "LON",      18, CoinType.Erc20("0x0000000000095413afc295d19edeb1ad7b71c952")),
                Coin("LOOM",        "Loom Network",         "LOOM",     18, CoinType.Erc20("0xA4e8C3Ec456107eA67d3075bF9e3DF3A75823DB0")),
                Coin("LRC",         "Loopring",             "LRC",      18, CoinType.Erc20("0xEF68e7C694F40c8202821eDF525dE3782458639f")),
                Coin("LTO",         "LTO Network",          "LTO",      8,  CoinType.Binance("LTO-BDF")),
                Coin("MFT",         "Mainframe Token",      "MFT",      18, CoinType.Erc20("0xdf2c7238198ad8b389666574f2d8bc411a4b7428")),
                Coin("MATIC",       "Matic Token",          "MATIC",    18, CoinType.Erc20("0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0")),
                Coin("MATIC-BEP2",  "Matic Token",          "MATIC",    8,  CoinType.Binance("MATIC-84A")),
                Coin("MKR",         "Maker",                "MKR",      18, CoinType.Erc20("0x9f8F72aA9304c8B593d555F12eF6589cC3A579A2")),
                Coin("MLN",         "Melon Token",          "MLN",      18, CoinType.Erc20("0xec67005c4e498ec7f55e092bd1d35cbc47c91892")),
                Coin("MET",         "Metronome",            "MET",      18, CoinType.Erc20("0xa3d58c4e56fedcae3a7c43a725aee9a71f0ece4e")),
                Coin("MCO",         "MCO",                  "MCO",      8,  CoinType.Erc20("0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d")),
                Coin("MCB",         "MCDEX Token",          "MCB",      18, CoinType.Erc20("0x4e352cF164E64ADCBad318C3a1e222E9EBa4Ce42")),
                Coin("MEME",        "MEME",                 "MEME",     8,  CoinType.Erc20("0xd5525d397898e5502075ea5e830d8914f6f0affe")),
                Coin("MTA",         "Meta",                 "MTA",      18, CoinType.Erc20("0xa3BeD4E1c75D00fa6f4E5E6922DB7261B5E9AcD2")),
                Coin("MUSD",        "mStable USD",          "MUSD",     18, CoinType.Erc20("0xe2f2a5c287993345a840db3b0845fbc70f5935a5")),
                Coin("TKN",         "Monolith",             "TKN",      8,  CoinType.Erc20("0xaaaf91d9b90df800df4f55c205fd6989c977e73a")),
                Coin("USDN",        "Neatrino USD",         "USDN",     18, CoinType.Erc20("0x674C6Ad92Fd080e4004b2312b45f796a192D27a0")),
                Coin("NEST",        "NEST",                 "NEST",     18, CoinType.Erc20("0x04abeda201850ac0124161f037efd70c74ddc74c")),
                Coin("NEXO",        "Nexo",                 "NEXO",     18, CoinType.Erc20("0xB62132e35a6c13ee1EE0f84dC5d40bad8d815206")),
                Coin("Nsure",       "Nsure Network Token",  "Nsure",    18, CoinType.Erc20("0x20945cA1df56D237fD40036d47E866C7DcCD2114")),
                Coin("NMR",         "Numeraire",            "NMR",      18, CoinType.Erc20("0x1776e1f26f98b1a5df9cd347953a26dd3cb46671")),
                Coin("NXM",         "NXM",                  "NXM",      18, CoinType.Erc20("0xd7c49cee7e9188cca6ad8ff264c1da2e69d4cf3b")),
                Coin("OCEAN",       "Ocean Token",          "OCEAN",    18, CoinType.Erc20("0x967da4048cD07aB37855c090aAF366e4ce1b9F48")),
                Coin("OCTO",        "Octo.fi",              "OCTO",     18, CoinType.Erc20("0x7240aC91f01233BaAf8b064248E80feaA5912BA3")),
                Coin("XFT",         "Offshift",             "XFT",      18, CoinType.Erc20("0xabe580e7ee158da464b51ee1a83ac0289622e6be")),
                Coin("COVER",       "Old Cover Protocol",   "COVER",    18, CoinType.Erc20("0x5D8d9F5b96f4438195BE9b99eee6118Ed4304286")),
                Coin("OMG",         "OmiseGO",              "OMG",      18, CoinType.Erc20("0xd26114cd6EE289AccF82350c8d8487fedB8A0C07")),
                Coin("ORAI",        "Oraichain Token",      "ORAI",     18, CoinType.Erc20("0x4c11249814f11b9346808179cf06e71ac328c1b5")),
                Coin("OGN",         "OriginToken",          "OGN",      18, CoinType.Erc20("0x8207c1ffc5b6804f6024322ccf34f29c3541ae26")),
                Coin("ORN",         "Orion Protocol",       "ORN",      8,  CoinType.Erc20("0x0258F474786DdFd37ABCE6df6BBb1Dd5dfC4434a")),
                Coin("ORBS",        "Orbs",                 "ORBS",     18, CoinType.Erc20("0xff56Cc6b1E6dEd347aA0B7676C85AB0B3D08B0FA")),
                Coin("OXT",         "Orchid",               "OXT",      18, CoinType.Erc20("0x4575f41308EC1483f3d399aa9a2826d74Da13Deb")),
                Coin("PAN",         "Panvala pan",          "PAN",      18, CoinType.Erc20("0xD56daC73A4d6766464b38ec6D91eB45Ce7457c44")),
                Coin("PAR",         "Parachute",            "PAR",      18, CoinType.Erc20("0x1beef31946fbbb40b877a72e4ae04a8d1a5cee06")),
                Coin("PAX",         "Paxos Standard",       "PAX",      18, CoinType.Erc20("0x8E870D67F660D95d5be530380D0eC0bd388289E1")),
                Coin("PERP",        "Perpetual",            "PERP",     18, CoinType.Erc20("0xbC396689893D065F41bc2C6EcbeE5e0085233447")),
                Coin("PICKLE",      "PickleToken",          "PICKLE",   18, CoinType.Erc20("0x429881672B9AE42b8EbA0E26cD9C73711b891Ca5")),
                Coin("PLOT",        "PLOT",                 "PLOT",     18, CoinType.Erc20("0x72F020f8f3E8fd9382705723Cd26380f8D0c66Bb")),
                Coin("POA",         "POA",                  "POA",      18, CoinType.Erc20("0x6758b7d441a9739b98552b373703d8d3d14f9e62")),
                Coin("POLS",        "PolkastarterToken",    "POLS",     18, CoinType.Erc20("0x83e6f1E41cdd28eAcEB20Cb649155049Fac3D5Aa")),
                Coin("POLY",        "Polymath",             "POLY",     18, CoinType.Erc20("0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC")),
                Coin("PPT",         "Populous",             "PPT",      8,  CoinType.Erc20("0xd4fa1460F537bb9085d22C7bcCB5DD450Ef28e3a")),
                Coin("pBTC",        "pTokens BTC",          "pBTC",     18, CoinType.Erc20("0x5228a22e72ccc52d415ecfd199f99d0665e7733b")),
                Coin("NPXS",        "Pundi X",              "NPXS",     18, CoinType.Erc20("0xA15C7Ebe1f07CaF6bFF097D8a589fb8AC49Ae5B3")),
                Coin("QNT",         "Quant",                "QNT",      18, CoinType.Erc20("0x4a220e6096b25eadb88358cb44068a3248254675")),
                Coin("QSP",         "Quantstamp",           "QSP",      18, CoinType.Erc20("0x99ea4db9ee77acd40b119bd1dc4e33e1c070b80d")),
                Coin("RDN",         "Raiden Network Token", "RDN",      18, CoinType.Erc20("0x255aa6df07540cb5d3d297f0d0d4d84cb52bc8e6")),
                Coin("RGT",         "Rari Governance Token","RGT",      18, CoinType.Erc20("0xD291E7a03283640FDc51b121aC401383A46cC623")),
                Coin("RENBTC",      "renBTC",               "renBTC",   8,  CoinType.Erc20("0xeb4c2781e4eba804ce9a9803c67d0893436bb27d")),
                Coin("RENBCH",      "renBCH",               "renBCH",   8,  CoinType.Erc20("0x459086f2376525bdceba5bdda135e4e9d3fef5bf")),
                Coin("RENZEC",      "renZEC",               "renZEC",   8,  CoinType.Erc20("0x1c5db575e2ff833e46a2e9864c22f4b22e0b37c2")),
                Coin("REN",         "Ren",                  "REN",      18, CoinType.Erc20("0x408e41876cccdc0f92210600ef50372656052a38")),
                Coin("RARI",        "Rarible",              "RARI",     18, CoinType.Erc20("0xfca59cd816ab1ead66534d82bc21e7515ce441cf")),
                Coin("RFI",         "reflect.finance",      "RFI",      9,  CoinType.Erc20("0xA1AFFfE3F4D611d252010E3EAf6f4D77088b0cd7")),
                Coin("REPv2",       "Reputation",           "REPv2",    8,  CoinType.Erc20("0x221657776846890989a759ba2973e427dff5c9bb")),
                Coin("RSR",         "Reserve Rights",       "RSR",      18, CoinType.Erc20("0x8762db106b2c2a0bccb3a80d1ed41273552616e8")),
                Coin("REV",         "Revain",               "REV",      0,  CoinType.Erc20("0x48f775EFBE4F5EcE6e0DF2f7b5932dF56823B990")),
                Coin("RFuel",       "Rio Fuel Token",       "RFuel",    18, CoinType.Erc20("0xaf9f549774ecedbd0966c52f250acc548d3f36e5")),
                Coin("XRP",         "Ripple",               "XRP",      8,  CoinType.Binance("XRP-BF2")),
                Coin("RLC",         "RLC",                  "RLC",      9,  CoinType.Erc20("0x607F4C5BB672230e8672085532f7e901544a7375")),
                Coin("XRT",         "Robonomics",           "XRT",      9,  CoinType.Erc20("0x7de91b204c1c737bcee6f000aaa6569cf7061cb7")),
                Coin("RPL",         "Rocket Pool",          "RPL",      18, CoinType.Erc20("0xb4efd85c19999d84251304bda99e90b92300bd93")),
                Coin("ROOT",        "RootKit",              "ROOT",     18, CoinType.Erc20("0xCb5f72d37685C3D5aD0bB5F982443BC8FcdF570E")),
                Coin("SAI",         "Sai",                  "SAI",      18, CoinType.Erc20("0x89d24A6b4CcB1B6fAA2625fE562bDD9a23260359")),
                Coin("SALT",        "Salt",                 "SALT",     8,  CoinType.Erc20("0x4156D3342D5c385a87D264F90653733592000581")),
                Coin("SAND",        "SAND",                 "SAND",     18, CoinType.Erc20("0x3845badAde8e6dFF049820680d1F14bD3903a5d0")),
                Coin("SAN",         "Santiment Network Token","SAN",    18, CoinType.Erc20("0x7c5a0ce9267ed19b22f8cae653f198e3e8daf098")),
                Coin("SHARE",       "Seigniorage Shares",   "SHARE",    9,  CoinType.Erc20("0x39795344CBCc76cC3Fb94B9D1b15C23c2070C66D")),
                Coin("KEY",         "SelfKey",              "KEY",      18, CoinType.Erc20("0x4cc19356f2d37338b9802aa8e8fc58b0373296e7")),
                Coin("SRM",         "Serum",                "SRM",      6,  CoinType.Erc20("0x476c5E26a75bd202a9683ffD34359C0CC15be0fF")),
                Coin("SHR",         "ShareToken",           "SHR",      8,  CoinType.Binance("SHR-DB6")),
                Coin("XOR",         "Sora",                 "XOR",      18, CoinType.Erc20("0x40FD72257597aA14C7231A7B1aaa29Fce868F677")),
                Coin("SPANK",       "SpankChain",           "SPANK",    18, CoinType.Erc20("0x42d6622dece394b54999fbd73d108123806f6a18")),
                Coin("SFI",         "Spice",                "SFI",      18, CoinType.Erc20("0xb753428af26e81097e7fd17f40c88aaa3e04902c")),
                Coin("SPDR",        "SpiderDAO Token",      "SPDR",     18, CoinType.Erc20("0xbcd4b7de6fde81025f74426d43165a5b0d790fdd")),
                Coin("EURS",        "STASIS EURO",          "EURS",     2,  CoinType.Erc20("0xdB25f211AB05b1c97D595516F45794528a807ad8")),
                Coin("SNT",         "Status",               "SNT",      18, CoinType.Erc20("0x744d70FDBE2Ba4CF95131626614a1763DF805B9E")),
                Coin("STORJ",       "Storj",                "STORJ",    8,  CoinType.Erc20("0xb64ef51c888972c908cfacf59b47c1afbc0ab8ac")),
                Coin("SURF",        "SURF.Finance",         "SURF",     18, CoinType.Erc20("0xea319e87cf06203dae107dd8e5672175e3ee976c")),
                Coin("SWFL",        "Swapfolio",            "SWFL",     18, CoinType.Erc20("0xBa21Ef4c9f433Ede00badEFcC2754B8E74bd538A")),
                Coin("SWRV",        "Swerve DAO Token",     "SWRV",     18, CoinType.Erc20("0xB8BAa0e4287890a5F79863aB62b7F175ceCbD433")),
                Coin("SXP",         "Swipe",                "SXP",      18, CoinType.Erc20("0x8ce9137d39326ad0cd6491fb5cc0cba0e089b6a9")),
                Coin("SWISS",       "Swiss Token",          "SWISS",    18, CoinType.Erc20("0x692eb773e0b5b7a79efac5a015c8b36a2577f65c")),
                Coin("CHSB",        "SwissBorg",            "CHSB",     8,  CoinType.Erc20("0xba9d4199fab4f26efe3551d490e3821486f135ba")),
                Coin("SNX",         "Synthetix",            "SNX",      18, CoinType.Erc20("0xC011a73ee8576Fb46F5E1c5751cA3B9Fe0af2a6F")),
                Coin("sETH",        "Synth sETH",           "sETH",     18, CoinType.Erc20("0x5e74C9036fb86BD7eCdcb084a0673EFc32eA31cb")),
                Coin("sUSD",        "Synth sUSD",           "sUSD",     18, CoinType.Erc20("0x57Ab1ec28D129707052df4dF418D58a2D46d5f51")),
                Coin("sXAU",        "Synth sXAU",           "sXAU",     18, CoinType.Erc20("0x261EfCdD24CeA98652B9700800a13DfBca4103fF")),
                Coin("TBTC",        "tBTC",                 "TBTC",     18, CoinType.Erc20("0x8daebade922df735c38c80c7ebd708af50815faa")),
                Coin("TRB",         "Tellor",               "TRB",      18, CoinType.Erc20("0x0ba45a8b5d5575935b8158a88c631e9f9c95a2e5")),
                Coin("USDT",        "Tether USD",           "USDT",     6,  CoinType.Erc20("0xdAC17F958D2ee523a2206206994597C13D831ec7")),
                Coin("FOR",         "The Force Token",      "FOR",      18, CoinType.Erc20("0x1fcdce58959f536621d76f5b7ffb955baa5a672f")),
                Coin("imBTC",       "The Tokenized Bitcoin","imBTC",    8,  CoinType.Erc20("0x3212b29E33587A00FB1C83346f5dBFA69A458923")),
                Coin("RUNE",        "THORChain",            "RUNE",     8,  CoinType.Binance("RUNE-B1A")),
                Coin("MTXLT",       "Tixl",                 "MTXLT",    8,  CoinType.Binance("MTXLT-286")),
                Coin("TAUD",        "TrueAUD",              "TAUD",     18, CoinType.Erc20("0x00006100F7090010005F1bd7aE6122c3C2CF0090")),
                Coin("TAUDB",       "TrueAUD",              "TAUDB",    8,  CoinType.Binance("TAUDB-888")),
                Coin("TCAD",        "TrueCAD",              "TCAD",     18, CoinType.Erc20("0x00000100F2A2bd000715001920eB70D229700085")),
                Coin("TGBP",        "TrueGBP",              "TGBP",     18, CoinType.Erc20("0x00000000441378008ea67f4284a57932b1c000a5")),
                Coin("THKD",        "TrueHKD",              "THKD",     18, CoinType.Erc20("0x0000852600ceb001e08e00bc008be620d60031f2")),
                Coin("THKDB",       "TrueHKD",              "THKDB",    8,  CoinType.Binance("THKDB-888")),
                Coin("TUSD",        "TrueUSD",              "TUSD",     18, CoinType.Erc20("0x0000000000085d4780B73119b644AE5ecd22b376")),
                Coin("TUSDB",       "TrueUSD",              "TUSDB",    8,  CoinType.Binance("TUSDB-888")),
                Coin("TRST",        "Trustcoin",            "TRST",     6,  CoinType.Erc20("0xcb94be6f13a1182e4a4b6140cb7bf2025d28e41b")),
                Coin("TRU",         "TrustToken",           "TRU",      8,  CoinType.Erc20("0x4c19596f5aaff459fa38b0f7ed92f11ae6543784")),
                Coin("SWAP",        "TrustSwap",            "SWAP",     18, CoinType.Erc20("0xCC4304A31d09258b0029eA7FE63d032f52e44EFe")),
                Coin("TWT",         "Trust Wallet",         "TWT",      8,  CoinType.Binance("TWT-8C2")),
                Coin("UBT",         "UniBright",            "UBT",      6,  CoinType.Erc20("0x8400d94a5cb0fa0d041a3788e395285d61c9ee5e")),
                Coin("SOCKS",       "Unisocks Edition 0",   "SOCKS",    18, CoinType.Erc20("0x23b608675a2b2fb1890d3abbd85c5775c51691d5")),
                Coin("UMA",         "UMA",                  "UMA",      18, CoinType.Erc20("0x04Fa0d235C4abf4BcF4787aF4CF447DE572eF828")),
                Coin("UNI",         "Uniswap",              "UNI",      18, CoinType.Erc20("0x1f9840a85d5af5bf1d1762f925bdaddc4201f984")),
                Coin("USDC",        "USD Coin",             "USDC",     6,  CoinType.Erc20("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48")),
                Coin("UTK",         "Utrust",               "UTK",      18, CoinType.Erc20("0xdc9Ac3C20D1ed0B540dF9b1feDC10039Df13F99c")),
                Coin("VERI",        "Veritaseum",           "VERI",     18, CoinType.Erc20("0x8f3470A7388c05eE4e7AF3d01D8C722b0FF52374")),
                Coin("WTC",         "Waltonchain",          "WTC",      18, CoinType.Erc20("0xb7cB1C96dB6B22b0D3d9536E0108d062BD488F74")),
                Coin("WAVES",       "WAVES",                "WAVES",    18, CoinType.Erc20("0x1cf4592ebffd730c7dc92c1bdffdfc3b9efcf29a")),
                Coin("WICC",        "WaykiChain Coin",      "WICC",     8,  CoinType.Binance("WICC-01D")),
                Coin("WRX",         "WazirX Token",         "WRX",      8,  CoinType.Binance("WRX-ED1")),
                Coin("WISE",        "Wise Token",           "WISE",     18, CoinType.Erc20("0x66a0f676479Cee1d7373f3DC2e2952778BfF5bd6")),
                Coin("WHITE",       "Whiteheart Token",     "WHITE",    18, CoinType.Erc20("0x5f0e628b693018f639d10e4a4f59bd4d8b2b6b44")),
                Coin("wANATHA",     "Wrapped ANATHA",       "wANATHA",  18, CoinType.Erc20("0x3383c5a8969Dc413bfdDc9656Eb80A1408E4bA20")),
                Coin("WBTC",        "Wrapped Bitcoin",      "WBTC",     8,  CoinType.Erc20("0x2260fac5e5542a773aa44fbcfedf7c193bc2c599")),
                Coin("WETH",        "Wrapped Ethereum",     "WETH",     18, CoinType.Erc20("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2")),
                Coin("WFIL",        "Wrapped Filecoin",     "WFIL",     18, CoinType.Erc20("0x6e1A19F235bE7ED8E3369eF73b196C07257494DE")),
                Coin("MIR",         "Wrapped MIR Token",    "MIR",      18, CoinType.Erc20("0x09a3EcAFa817268f77BE1283176B946C4ff2E608")),
                Coin("WZEC",        "Wrapped ZEC",          "WZEC",     18, CoinType.Erc20("0x4a64515e5e1d1073e83f30cb97bed20400b66e10")),
                Coin("STAKE",       "xDAI",                 "STAKE",    18, CoinType.Erc20("0x0Ae055097C6d159879521C384F1D2123D1f195e6")),
                Coin("XIO",         "XIO Network",          "XIO",      18, CoinType.Erc20("0x0f7F961648aE6Db43C75663aC7E5414Eb79b5704")),
                Coin("YAX",         "yAxis",                "YAX",      18, CoinType.Erc20("0xb1dc9124c395c1e97773ab855d66e879f053a289")),
                Coin("YFI",         "yearn.finance",        "YFI",      18, CoinType.Erc20("0x0bc529c00c6401aef6d220be8c6ea1667f6ad93e")),
                Coin("Yf-DAI",      "YfDAI.finance",        "Yf-DAI",   18, CoinType.Erc20("0xf4CD3d3Fda8d7Fd6C5a500203e38640A70Bf9577")),
                Coin("YFII",        "YFII.finance",         "YFII",     18, CoinType.Erc20("0xa1d0E215a23d7030842FC67cE582a6aFa3CCaB83")),
                Coin("YFIM",        "yfi.mobi",             "YFIM",     18, CoinType.Erc20("0x2e2f3246b6c65ccc4239c9ee556ec143a7e5de2c")),
                Coin("ZAI",         "Zero Collateral Dai",  "ZAI",      18, CoinType.Erc20("0x9d1233cc46795E94029fDA81aAaDc1455D510f15")),


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
