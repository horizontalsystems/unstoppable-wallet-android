package io.horizontalsystems.bankwallet.core.providers

import android.os.Parcelable
import androidx.room.Entity
import com.binance.connector.client.exceptions.BinanceClientException
import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.ActiveAccountState
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.bankwallet.modules.balance.cex.CexAddress
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class CexProviderManager(private val accountManager: IAccountManager) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _cexProviderFlow = MutableStateFlow<ICexProvider?>(null)
    val cexProviderFlow = _cexProviderFlow.asStateFlow()

    init {
        coroutineScope.launch {
            accountManager.activeAccountStateFlow.collect {
                handleAccount((it as? ActiveAccountState.ActiveAccount)?.account)
            }
        }
    }

    private fun handleAccount(account: Account?) {
        val cexProvider = when (val cexType = (account?.type as? AccountType.Cex)?.cexType) {
            is CexType.Binance -> BinanceCexProvider(cexType.apiKey, cexType.secretKey, account)
            null -> null
        }

        _cexProviderFlow.update { cexProvider }
    }
}

interface ICexProvider {
    val account: Account

    suspend fun getAssets(): List<CexAssetRaw>
    suspend fun getAddress(assetId: String, networkId: String?): CexAddress
}

@Entity(primaryKeys = ["id", "accountId"])
data class CexAssetRaw(
    val id: String,
    val accountId: String,
    val name: String,
    val freeBalance: BigDecimal,
    val lockedBalance: BigDecimal,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val depositNetworks: List<CexDepositNetworkRaw>,
    val withdrawNetworks: List<CexWithdrawNetworkRaw>,
    val coinUid: String?,
    val decimals: Int,
)

@Parcelize
data class CexAsset(
    val id: String,
    val name: String,
    val freeBalance: BigDecimal,
    val lockedBalance: BigDecimal,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val depositNetworks: List<CexDepositNetwork>,
    val withdrawNetworks: List<CexWithdrawNetwork>,
    val coin: Coin?,
    val decimals: Int,
) : Parcelable

data class CexDepositNetworkRaw(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val enabled: Boolean,
    val minAmount: BigDecimal,
    val blockchainUid: String?,
) {
    fun cexDepositNetwork(blockchain: Blockchain?) =
        CexDepositNetwork(
            id = id,
            name = name,
            isDefault = isDefault,
            enabled = enabled,
            minAmount = minAmount,
            blockchain = blockchain
        )
}

@Parcelize
data class CexDepositNetwork(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val enabled: Boolean,
    val minAmount: BigDecimal,
    val blockchain: Blockchain?,
) : Parcelable {
    val networkName get() = blockchain?.name ?: name
}

data class CexWithdrawNetworkRaw(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val enabled: Boolean,
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal,
    val fixedFee: BigDecimal,
    val feePercent: BigDecimal,
    val minFee: BigDecimal,
    val blockchainUid: String?,
) {
    fun cexWithdrawNetwork(blockchain: Blockchain?) =
        CexWithdrawNetwork(
            id = id,
            name = name,
            isDefault = isDefault,
            enabled = enabled,
            minAmount = minAmount,
            maxAmount = maxAmount,
            fixedFee = fixedFee,
            feePercent = feePercent,
            minFee = minFee,
            blockchain = blockchain
        )
}

@Parcelize
data class CexWithdrawNetwork(
    val id: String,
    val name: String,
    val isDefault: Boolean,
    val enabled: Boolean,
    val minAmount: BigDecimal,
    val maxAmount: BigDecimal,
    val fixedFee: BigDecimal,
    val feePercent: BigDecimal,
    val minFee: BigDecimal,
    val blockchain: Blockchain?,
) : Parcelable {
    val networkName get() = blockchain?.name ?: name
}

class BinanceCexProvider(apiKey: String, secretKey: String, override val account: Account) : ICexProvider {
    private val client = SpotClientImpl(apiKey, secretKey)
    private val wallet = client.createWallet()
    private val gson = Gson()

    private val coinUidMap = mapOf(
        "1INCH" to "1inch",
        "AAVE" to "aave",
        "ACA" to "acala",
        "ACH" to "alchemy-pay",
        "ACM" to "ac-milan-fan-token",
        "ADA" to "cardano",
        "ADX" to "adex",
        "AERGO" to "aergo",
        "AGIX" to "singularitynet",
        "AGLD" to "adventure-gold",
        "AKRO" to "akropolis",
        "ALCX" to "alchemix",
        "ALGO" to "algorand",
        "ALICE" to "my-neighbor-alice",
        "ALPACA" to "alpaca-finance",
        "ALPHA" to "alpha-finance",
        "ALPINE" to "alpine-f1-team-fan-token",
        "AMB" to "amber",
        "AMP" to "amp-token",
        "ANKR" to "ankr",
        "ANT" to "aragon",
        "APE" to "apecoin-ape",
        "API3" to "api3",
        "APT" to "aptos",
        "AR" to "arweave",
        "ARB" to "arbitrum",
        "ARDR" to "ardor",
        "ARK" to "ark",
        "ARKM" to "arkham",
        "ARPA" to "arpa",
        "ASR" to "as-roma-fan-token",
        "AST" to "airswap",
        "ASTR" to "astar",
        "ATA" to "automata",
        "ATM" to "atletico-madrid",
        "ATOM" to "cosmos",
        "AUCTION" to "auction",
        "AUDIO" to "audius",
        "AUTO" to "cube",
        "AVA" to "concierge-io",
        "AVAX" to "avalanche-2",
        "AXS" to "axie-infinity",
        "BADGER" to "badger-dao",
        "BAKE" to "bakerytoken",
        "BAL" to "balancer",
        "BAND" to "band-protocol",
        "BAR" to "fc-barcelona-fan-token",
        "BAT" to "basic-attention-token",
        "BCH" to "bitcoin-cash",
        "BDOT" to "babydot",
        "BEL" to "bella-protocol",
        "BETA" to "beta-finance",
        "BETH" to "binance-eth",
        "BICO" to "biconomy",
        "BIDR" to "binanceidr",
        "BIFI" to "beefy-finance",
        "BLZ" to "bluzelle",
        "BNB" to "binancecoin",
        "BNC" to "bifrost-native-coin",
        "BNT" to "bancor",
        "BNX" to "binaryx",
        "BOND" to "barnbridge",
        "BSW" to "biswap",
        "BTC" to "bitcoin",
        "BTS" to "bitshares",
        "BTTOLD" to "bittorrent-old",
        "BURGER" to "burger-swap",
        "BUSD" to "binance-usd",
        "C98" to "coin98",
        "CAKE" to "pancakeswap-token",
        "CAN" to "channels",
        "CELO" to "celo",
        "CELR" to "celer-network",
        "CFX" to "conflux-token",
        "CHESS" to "tranchess",
        "CHR" to "chromaway",
        "CHZ" to "chiliz",
        "CITY" to "manchester-city-fan-token",
        "CKB" to "nervos-network",
        "CLV" to "clover-finance",
        "COCOS" to "cocos-bcx",
        "COMBO" to "furucombo",
        "COMP" to "compound-governance-token",
        "COS" to "contentos",
        "COTI" to "coti",
        "CREAM" to "cream-2",
        "CRV" to "curve-dao-token",
        "CTK" to "certik",
        "CTSI" to "cartesi",
        "CTXC" to "cortex",
        "CVC" to "civic",
        "CVP" to "concentrated-voting-power",
        "CVX" to "convex-finance",
        "DAI" to "dai",
        "DAR" to "mines-of-dalarnia",
        "DASH" to "dash",
        "DATA" to "streamr",
        "DCR" to "decred",
        "DEGO" to "dego-finance",
        "DENT" to "dent",
        "DEXE" to "dexe",
        "DF" to "dforce-token",
        "DGB" to "digibyte",
        "DIA" to "dia-data",
        "DOCK" to "dock",
        "DODO" to "dodo",
        "DOGE" to "dogecoin",
        "DOT" to "polkadot",
        "DREP" to "drep-new",
        "DUSK" to "dusk-network",
        "DYDX" to "dydx",
        "EDU" to "edu-coin",
        "EFI" to "efinity",
        "EGLD" to "elrond-erd-2",
        "ELF" to "aelf",
        "ENJ" to "enjincoin",
        "ENS" to "ethereum-name-service",
        "EOS" to "eos",
        "EPS" to "ellipsis",
        "ERN" to "ethernity-chain",
        "ETC" to "ethereum-classic",
        "ETH" to "ethereum",
        "ETHDOWN" to "ethdown",
        "ETHUP" to "ethup",
        "ETHW" to "ethereum-pow-iou",
        "EVX" to "everex",
        "FARM" to "harvest-finance",
        "FET" to "fetch-ai",
        "FIDA" to "bonfida",
        "FIL" to "filecoin",
        "FIO" to "fio-protocol",
        "FIRO" to "zcoin",
        "FIS" to "stafi",
        "FLM" to "flamingo-finance",
        "FLOKI" to "floki",
        "FLOW" to "flow",
        "FLR" to "flare-networks",
        "FLUX" to "zelcash",
        "FOR" to "force-protocol",
        "FORTH" to "ampleforth-governance-token",
        "FRONT" to "frontier-token",
        "FTM" to "fantom",
        "FTT" to "ftx-token",
        "FUN" to "funfair",
        "FXS" to "frax-share",
        "GAL" to "project-galaxy",
        "GALA" to "gala",
        "GAS" to "gas",
        "GFT" to "game-fantasy-token",
        "GHST" to "aavegotchi",
        "GLM" to "golem",
        "GLMR" to "moonbeam",
        "GMT" to "stepn",
        "GMX" to "gmx",
        "GNO" to "gnosis",
        "GNS" to "gains-network",
        "GRT" to "the-graph",
        "GTC" to "gitcoin",
        "GYEN" to "gyen",
        "HARD" to "kava-lend",
        "HBAR" to "hedera-hashgraph",
        "HFT" to "hashflow",
        "HIFI" to "hifi-finance",
        "HIGH" to "highstreet",
        "HIVE" to "hive",
        "HOOK" to "hooked-protocol",
        "HOT" to "holotoken",
        "ICP" to "internet-computer",
        "ICX" to "icon",
        "ID" to "space-id",
        "IDEX" to "aurora-dao",
        "IDRT" to "rupiah-token",
        "ILV" to "illuvium",
        "IMX" to "immutable-x",
        "INJ" to "injective-protocol",
        "IOST" to "iostoken",
        "IOTX" to "iotex",
        "IQ" to "everipedia",
        "IRIS" to "iris-network",
        "JASMY" to "jasmycoin",
        "JOE" to "joe",
        "JST" to "just",
        "JUV" to "juventus-fan-token",
        "KAVA" to "kava",
        "KDA" to "kadena",
        "KEY" to "selfkey",
        "KEYFI" to "keyfi",
        "KLAY" to "klay-token",
        "KMD" to "komodo",
        "KNC" to "kyber-network-crystal",
        "KNCL" to "kyber-network",
        "KP3R" to "keep3rv1",
        "KSM" to "kusama",
        "LAZIO" to "lazio-fan-token",
        "LBA" to "libra-credit",
        "LDO" to "lido-dao",
        "LEVER" to "lever",
        "LINA" to "linear",
        "LINK" to "chainlink",
        "LIT" to "litentry",
        "LOKA" to "league-of-kingdoms",
        "LOOKS" to "looksrare",
        "LOOM" to "loom-network-new",
        "LPT" to "livepeer",
        "LQTY" to "liquity",
        "LRC" to "loopring",
        "LSK" to "lisk",
        "LTC" to "litecoin",
        "LTO" to "lto-network",
        "LUNA" to "terra-luna-2",
        "MAGIC" to "magic",
        "MANA" to "decentraland",
        "MASK" to "mask-network",
        "MATIC" to "matic-network",
        "MBL" to "moviebloc",
        "MBOX" to "mobox",
        "MC" to "merit-circle",
        "MDT" to "measurable-data-token",
        "MDX" to "mdex",
        "MINA" to "mina-protocol",
        "MKR" to "maker",
        "MLN" to "melon",
        "MOB" to "mobilecoin",
        "MOVR" to "moonriver",
        "MTL" to "metal",
        "MTLX" to "mettalex",
        "MULTI" to "multichain",
        "NEAR" to "near",
        "NEBL" to "neblio",
        "NEO" to "neo",
        "NEXO" to "nexo",
        "NFT" to "apenft",
        "NKN" to "nkn",
        "NMR" to "numeraire",
        "NULS" to "nuls",
        "NVT" to "nervenetwork",
        "OAX" to "openanx",
        "OCEAN" to "ocean-protocol",
        "OG" to "og-fan-token",
        "OGN" to "origin-protocol",
        "OM" to "mantra-dao",
        "OMG" to "omisego",
        "ONE" to "harmony",
        "ONT" to "ontology",
        "OOKI" to "ooki",
        "OP" to "optimism",
        "ORN" to "orion-protocol",
        "OSMO" to "osmosis",
        "OXT" to "orchid-protocol",
        "PARA" to "paralink-network",
        "PAXG" to "pax-gold",
        "PEOPLE" to "constitutiondao",
        "PEPE" to "pepe",
        "PERL" to "perlin",
        "PERP" to "perpetual-protocol",
        "PHA" to "pha",
        "PHB" to "phoenix-global",
        "PIVX" to "pivx",
        "PLA" to "playdapp",
        "PNT" to "pnetwork",
        "POLS" to "polkastarter",
        "POLYX" to "polymesh",
        "POND" to "marlin",
        "PORTO" to "fc-porto",
        "POWR" to "power-ledger",
        "PROM" to "prometeus",
        "PROS" to "prosper",
        "PSG" to "paris-saint-germain-fan-token",
        "PUNDIX" to "pundi-x-2",
        "PYR" to "vulcan-forged",
        "QI" to "benqi",
        "QKC" to "quark-chain",
        "QLC" to "qlink",
        "QNT" to "quant-network",
        "QTUM" to "qtum",
        "QUICK" to "quickswap",
        "RAD" to "radicle",
        "RARE" to "superrare",
        "RAY" to "raydium",
        "RCN" to "ripio-credit-network",
        "RDNT" to "radiant-capital",
        "REEF" to "reef",
        "REI" to "rei-network",
        "REN" to "republic-protocol",
        "REQ" to "request-network",
        "RIF" to "rif-token",
        "RLC" to "iexec-rlc",
        "RNDR" to "render-token",
        "ROSE" to "oasis-network",
        "RPL" to "rocket-pool",
        "RSR" to "reserve-rights-token",
        "RUNE" to "thorchain",
        "RVN" to "ravencoin",
        "SAND" to "the-sandbox",
        "SANTOS" to "santos-fc-fan-token",
        "SC" to "siacoin",
        "SCRT" to "secret",
        "SFP" to "safepal",
        "SHIB" to "shiba-inu",
        "SKL" to "skale",
        "SLP" to "smooth-love-potion",
        "SNM" to "sonm",
        "SNT" to "status",
        "SNX" to "havven",
        "SOL" to "solana",
        "SOLO" to "solo-coin",
        "SPELL" to "spell-token",
        "SRM" to "serum",
        "SSV" to "ssv-network",
        "STEEM" to "steem",
        "STG" to "stargate-finance",
        "STMX" to "storm",
        "STORJ" to "storj",
        "STPT" to "stp-network",
        "STRAX" to "stratis",
        "STX" to "blockstack",
        "SUI" to "sui",
        "SUN" to "sun-token",
        "SUPER" to "superfarm",
        "SUSHI" to "sushi",
        "SXP" to "swipe",
        "SYN" to "synapse-2",
        "SYS" to "syscoin",
        "TCT" to "tokenclub",
        "TFUEL" to "theta-fuel",
        "THETA" to "theta-token",
        "TKO" to "tokocrypto",
        "TLM" to "alien-worlds",
        "TOMO" to "tomochain",
        "TORN" to "tornado-cash",
        "TRB" to "tellor",
        "TROY" to "troy",
        "TRU" to "truefi",
        "TRX" to "tron",
        "TUSD" to "true-usd",
        "TVK" to "the-virtua-kolect",
        "TWT" to "trust-wallet-token",
        "UFT" to "unlend-finance",
        "UMA" to "uma",
        "UNFI" to "unifi-protocol-dao",
        "UNI" to "uniswap",
        "USDC" to "usd-coin",
        "USDP" to "paxos-standard",
        "USDT" to "tether",
        "UTK" to "utrust",
        "VAI" to "vai",
        "VET" to "vechain",
        "VGX" to "ethos",
        "VIB" to "viberate",
        "VIDT" to "vidt-dao",
        "VITE" to "vite",
        "VOXEL" to "voxies",
        "VRT" to "venus-reward-token",
        "VTHO" to "vethor-token",
        "WAN" to "wanchain",
        "WAVES" to "waves",
        "WAXP" to "wax",
        "WBNB" to "wbnb",
        "WBTC" to "wrapped-bitcoin",
        "WETH" to "weth",
        "WIN" to "wink",
        "WING" to "wing-finance",
        "WNXM" to "wrapped-nxm",
        "WOO" to "woo-network",
        "WRX" to "wazirx",
        "WTC" to "waltonchain",
        "XEC" to "ecash",
        "XEM" to "nem",
        "XLM" to "stellar",
        "XMR" to "monero",
        "XNO" to "nano",
        "XRP" to "ripple",
        "XTZ" to "tezos",
        "XVG" to "verge",
        "XVS" to "venus",
        "YFI" to "yearn-finance",
        "YFII" to "yfii-finance",
        "YGG" to "yield-guild-games",
        "ZEC" to "zcash",
        "ZEN" to "zencash",
        "ZIL" to "zilliqa",
        "ZRX" to "0x",
    )

    private val blockchainUidMap = mapOf(
        "BTC" to "bitcoin",
        "BSC" to "binance-smart-chain",
        "ETH" to "ethereum",
        "EOS" to "eos",
        "NEAR" to "near-protocol",
        "AVAXC" to "avalanche",
        "ARBITRUM" to "arbitrum-one",
        "BNB" to "binancecoin",
        "OPTIMISM" to "optimistic-ethereum",
        "MATIC" to "polygon-pos",
        "STATEMINT" to "polkadot",
        "SOL" to "solana",
        "XTZ" to "tezos",
        "TRX" to "tron",
        "APT" to "aptos",
        "ADA" to "cardano",
        "FTM" to "fantom",
        "BCH" to "bitcoin-cash",
        "ETC" to "ethereum-classic",
        "FIL" to "filecoin",
        "FLOW" to "flow",
        "LTC" to "litecoin",
        "XRP" to "ripple",
        "ZEC" to "zcash",
    )

    override suspend fun getAddress(assetId: String, networkId: String?): CexAddress {
        val params = buildMap<String, Any> {
            put("coin", assetId)
            networkId?.let {
                put("network", it)
            }
        }.toMutableMap()

        val depositAddress = gson.fromJson(wallet.depositAddress(params), DepositAddress::class.java)
        return CexAddress(depositAddress.address, depositAddress.tag)
    }

    override suspend fun getAssets(): List<CexAssetRaw> {
        val coinInfo = gson.fromJson<List<CoinInfo>>(
            wallet.coinInfo(mutableMapOf()),
            object : TypeToken<List<CoinInfo>>() {}.type
        )

        return coinInfo
            .filter { !it.isLegalMoney }
            .map {
                val assetId = it.coin
                CexAssetRaw(
                    id = assetId,
                    accountId = account.id,
                    name = it.name,
                    freeBalance = it.free,
                    lockedBalance = it.freeze,
                    depositEnabled = it.depositAllEnable,
                    withdrawEnabled = false, /*it.withdrawAllEnable*/
                    depositNetworks = it.networkList.map { coinNetwork ->
                        CexDepositNetworkRaw(
                            id = coinNetwork.network,
                            name = coinNetwork.name,
                            isDefault = coinNetwork.isDefault,
                            enabled = coinNetwork.depositEnable,
                            minAmount = BigDecimal.ZERO,
                            blockchainUid = blockchainUidMap[coinNetwork.network]
                        )
                    },
                    withdrawNetworks = it.networkList.map { coinNetwork ->
                        CexWithdrawNetworkRaw(
                            id = coinNetwork.network,
                            name = coinNetwork.name,
                            isDefault = coinNetwork.isDefault,
                            enabled = false, /*coinNetwork.withdrawEnable*/
                            minAmount = coinNetwork.withdrawMin,
                            maxAmount = coinNetwork.withdrawMax,
                            fixedFee = coinNetwork.withdrawFee,
                            feePercent = BigDecimal.ZERO,
                            minFee = BigDecimal.ZERO,
                            blockchainUid = blockchainUidMap[coinNetwork.network]
                        )
                    },
                    coinUid = coinUidMap[assetId],
                    decimals = 8
                )
            }
    }

    data class CoinInfo(
        val coin: String,
        val name: String,
        val free: BigDecimal,
        val freeze: BigDecimal,
        val depositAllEnable: Boolean,
        val withdrawAllEnable: Boolean,
        val networkList: List<CoinNetwork>,
        val isLegalMoney: Boolean
    )

    data class CoinNetwork(
        val name: String,
        val network: String,
        val isDefault: Boolean,
        val depositEnable: Boolean,
        val withdrawEnable: Boolean,
        val withdrawFee: BigDecimal,
        val withdrawMin: BigDecimal,
        val withdrawMax: BigDecimal,
    )

    data class DepositAddress(
        val address: String,
        val coin: String,
        val tag: String,
        val url: String,
    )

    companion object {
        @Throws(BinanceClientException::class)
        fun validate(apiKey: String, secretKey: String) {
            val client = SpotClientImpl(apiKey, secretKey)
            val wallet = client.createWallet()
            wallet.coinInfo(mutableMapOf())
        }
    }

}