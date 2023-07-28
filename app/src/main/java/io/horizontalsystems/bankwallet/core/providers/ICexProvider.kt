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
import io.horizontalsystems.bankwallet.modules.balance.cex.CoinzixCexApiService
import io.horizontalsystems.bankwallet.modules.balance.cex.Response
import io.horizontalsystems.bankwallet.modules.coinzixverify.CoinzixVerificationMode
import io.horizontalsystems.bankwallet.modules.coinzixverify.TwoFactorType
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
        val cexType = (account?.type as? AccountType.Cex)?.cexType
        val cexProvider = when (cexType) {
            is CexType.Binance -> BinanceCexProvider(cexType.apiKey, cexType.secretKey, account)
            is CexType.Coinzix -> CoinzixCexProvider(cexType.authToken, cexType.secret, account)
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

class CoinzixCexProvider(
    private val authToken: String,
    private val secret: String,
    override val account: Account
) : ICexProvider {
    private val api = CoinzixCexApiService()

    private val coinUidMap = mapOf(
        "1INCH" to "1inch",
        "AAVE" to "aave",
        "ADA" to "cardano",
        "ALGO" to "algorand",
        "AMP" to "amp-token",
        "APE" to "apecoin-ape",
        "ARB" to "arbitrum",
        "ATOM" to "cosmos",
        "AVAX" to "avalanche-2",
        "AXS" to "axie-infinity",
        "BAKE" to "bakerytoken",
        "BCH" to "bitcoin-cash",
        "BNB" to "binancecoin",
        "BTC" to "bitcoin",
        "BUSD" to "binance-usd",
        "CAKE" to "pancakeswap-token",
        "CHZ" to "chiliz",
        "COMP" to "compound-governance-token",
        "DENT" to "dent",
        "DOGE" to "dogecoin",
        "DOT" to "polkadot",
        "EGLD" to "elrond-erd-2",
        "ENJ" to "enjincoin",
        "EOS" to "eos",
        "ETC" to "ethereum-classic",
        "ETH" to "ethereum",
        "FIL" to "filecoin",
        "FLOKI" to "floki",
        "FTM" to "fantom",
        "GALA" to "gala",
        "GMT" to "stepn",
        "GRT" to "the-graph",
        "HOT" to "holotoken",
        "IOTA" to "iota",
        "LINK" to "chainlink",
        "LTC" to "litecoin",
        "LUNA" to "terra-luna-2",
        "LUNC" to "terra-luna",
        "MANA" to "decentraland",
        "MATIC" to "matic-network",
        "MKR" to "maker",
        "NEAR" to "near",
        "ONE" to "harmony",
        "PEPE" to "pepe",
        "QNT" to "quant-network",
        "QTUM" to "qtum",
        "REEF" to "reef",
        "RNDR" to "render-token",
        "RUNE" to "thorchain",
        "SAND" to "the-sandbox",
        "SC" to "siacoin",
        "SHIB" to "shiba-inu",
        "SOL" to "solana",
        "SUI" to "sui",
        "SUSHI" to "sushi",
        "TFUEL" to "theta-fuel",
        "THETA" to "theta-token",
        "TRX" to "tron",
        "UNI" to "uniswap",
        "USDT" to "tether",
        "VET" to "vechain",
        "WIN" to "wink",
        "WISTA" to "wistaverse",
        "XLM" to "stellar",
        "XMR" to "monero",
        "XRP" to "ripple",
        "XTZ" to "tezos",
        "XVG" to "verge",
        "YFI" to "yearn-finance",
        "ZIL" to "zilliqa",
        "ZIX" to "coinzix-token",
    )

    private val networkTypeToBlockchainUidMap = mapOf(
        1 to "ethereum",
        2 to "tron",
        3 to "binancecoin",
        4 to "binance-smart-chain",
        6 to "solana",
        8 to "polygon-pos",
        9 to "arbitrum-one",
    )

    private val isoToBlockchainUidMap = mapOf(
        "ADA" to "cardano",
        "ALGO" to "algorand",
        "ATOM" to "cosmos",
        "BCH" to "bitcoin-cash",
        "BTC" to "bitcoin",
        "DOGE" to "dogecoin",
        "DOT" to "polkadot",
        "EGLD" to "elrond-erd-2",
        "EOS" to "eos",
        "ETH" to "ethereum",
        "LTC" to "litecoin",
        "LUNA" to "terra-luna-2",
        "LUNC" to "terra-luna",
        "MATIC" to "polygon-pos",
        "ONE" to "harmony",
        "QTUM" to "qtum",
        "RUNE" to "thorchain",
        "SC" to "siacoin",
        "SOL" to "solana",
        "SUI" to "sui",
        "THETA" to "theta-token",
        "TRX" to "tron",
        "VET" to "vechain",
        "XLM" to "stellar",
        "XMR" to "monero",
        "XRP" to "ripple",
        "ZIL" to "zilliqa",
    )

    private val nativeNetworkId = "NATIVE"


    override suspend fun getAddress(assetId: String, networkId: String?): CexAddress {
        val network = if (networkId == nativeNetworkId) null else networkId
        val addressData = api.getAddress(authToken, secret, assetId, 0, network)

        return when {
            addressData.address != null -> {
                CexAddress(addressData.address, "")
            }

            addressData.account != null -> {
                CexAddress(addressData.account, addressData.memo ?: "")
            }

            else -> {
                throw Exception()
            }
        }
    }

    suspend fun withdraw(
        assetId: String,
        networkId: String?,
        address: String,
        amount: BigDecimal,
        feeFromAmount: Boolean
    ): CoinzixVerificationMode.Withdraw {
        val network = if (networkId == nativeNetworkId) null else networkId

        val response = api.withdraw(authToken, secret, assetId, network, address, amount, feeFromAmount)
        validate(response)

        val data = response.data ?: throw IllegalStateException("Withdraw response data is null")

        return CoinzixVerificationMode.Withdraw(data.id, data.step.mapNotNull { TwoFactorType.fromCode(it) })
    }

    suspend fun confirmWithdraw(
        withdrawId: String,
        emailCode: String?,
        twoFactorCode: String?
    ) {
        val response = api.confirmWithdraw(authToken, secret, withdrawId, emailCode, twoFactorCode)
        validate(response)
    }

    suspend fun sendWithdrawPin(
        withdrawId: String
    ) {
        val response = api.sendWithdrawPin(authToken, secret, withdrawId)
        validate(response)
    }

    private fun validate(response: Response.Withdraw) {
        check(response.status) { response.errors?.joinToString { it } ?: response.message ?: "Unknown error" }
    }

    override suspend fun getAssets(): List<CexAssetRaw> {
        val configResponse = api.getConfig()
        val ignoredIds = configResponse.data.fiat_currencies + configResponse.data.demo_currency.values

        return api.getBalances(authToken, secret)
            .mapNotNull {
                val assetId = it.currency.iso3

                if (ignoredIds.contains(assetId)) return@mapNotNull null

                val decimals = 8
                val depositEnabled = configResponse.data.currency_deposit.contains(assetId)
                val withdrawEnabled = configResponse.data.currency_withdraw.contains(assetId)

                CexAssetRaw(
                    id = assetId,
                    accountId = account.id,
                    name = it.currency.name,
                    freeBalance = it.balance_available.movePointLeft(decimals),
                    lockedBalance = (it.balance - it.balance_available).movePointLeft(decimals),
                    depositEnabled = depositEnabled,
                    withdrawEnabled = withdrawEnabled,
                    depositNetworks = configResponse.depositNetworks(assetId).mapIndexedNotNull { index, depositNetwork ->
                        val networkId = if (depositNetwork.network_type == 0) {
                            nativeNetworkId
                        } else {
                            it.currency.networks[depositNetwork.network_type]
                        }
                        networkId?.let {
                            CexDepositNetworkRaw(
                                id = networkId,
                                name = networkId,
                                isDefault = index == 0,
                                enabled = depositEnabled,
                                minAmount = depositNetwork.min_refill,
                                blockchainUid = if (depositNetwork.network_type == 0)
                                    isoToBlockchainUidMap[assetId]
                                else
                                    networkTypeToBlockchainUidMap[depositNetwork.network_type]
                            )
                        }
                    },
                    withdrawNetworks = configResponse.withdrawNetworks(assetId).mapIndexedNotNull { index, withdrawNetwork ->
                        val networkId = if (withdrawNetwork.network_type == 0) {
                            nativeNetworkId
                        } else {
                            it.currency.networks[withdrawNetwork.network_type]
                        }

                        networkId?.let {
                            CexWithdrawNetworkRaw(
                                id = networkId,
                                name = networkId,
                                isDefault = index == 0,
                                enabled = withdrawEnabled,
                                minAmount = withdrawNetwork.min_withdraw,
                                maxAmount = withdrawNetwork.max_withdraw,
                                fixedFee = withdrawNetwork.fixed,
                                feePercent = withdrawNetwork.percent,
                                minFee = withdrawNetwork.min_commission,
                                blockchainUid = if (withdrawNetwork.network_type == 0)
                                    isoToBlockchainUidMap[assetId]
                                else
                                    networkTypeToBlockchainUidMap[withdrawNetwork.network_type]
                            )
                        }
                    },
                    coinUid = coinUidMap[assetId],
                    decimals = decimals
                )
            }
    }
}


class BinanceCexProvider(apiKey: String, secretKey: String, override val account: Account) : ICexProvider {
    private val client = SpotClientImpl(apiKey, secretKey)
    private val wallet = client.createWallet()
    private val gson = Gson()

    private val coinUidMap = mapOf(
        "USDT" to "tether",
        "BUSD" to "binance-usd",
        "AGIX" to "singularitynet",
        "SUSHI" to "sushi",
        "GMT" to "stepn",
        "CAKE" to "pancakeswap-token",
        "ETH" to "ethereum",
        "ETHW" to "ethereum-pow-iou",
        "BTC" to "bitcoin",
        "BNB" to "binancecoin",
        "SOL" to "solana",
        "QI" to "benqi",
        "BSW" to "biswap",
    )

    private val blockchainUidMap = mapOf(
        "BSC" to "binance-smart-chain",
        "ETH" to "ethereum",
    )

    override suspend fun getAddress(assetId: String, networkId: String?): CexAddress {
        val params = buildMap<String, Any> {
            put("coin", assetId)
            networkId?.let {
                put("network", it)
            }
        }.toMutableMap()

        val depositAddress = gson.fromJson(wallet.depositAddress(params), BinanceCexProvider.DepositAddress::class.java)
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