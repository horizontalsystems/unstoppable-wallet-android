package io.horizontalsystems.bankwallet.core.providers

import android.os.Parcelable
import androidx.room.Entity
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
    val networks: List<CexNetworkRaw>,
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
    val networks: List<CexNetwork>,
    val coin: Coin?,
    val decimals: Int,
) : Parcelable

data class CexNetworkRaw(
    val network: String,
    val name: String,
    val isDefault: Boolean,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val blockchainUid: String?,
)

@Parcelize
data class CexNetwork(
    val network: String,
    val name: String,
    val isDefault: Boolean,
    val depositEnabled: Boolean,
    val withdrawEnabled: Boolean,
    val blockchain: Blockchain?,
) : Parcelable

class CoinzixCexProvider(
    private val authToken: String,
    private val secret: String,
    override val account: Account
) : ICexProvider {
    private val api = CoinzixCexApiService()

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
        "SOL" to "solana",
        "BNB" to "binancecoin",
        "MATIC" to "polygon-pos",
        "TRX" to "tron",
    )

    override suspend fun getAddress(assetId: String, networkId: String?): CexAddress {
        val addressData = api.getAddress(authToken, secret, assetId, 0, networkId)

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

    override suspend fun getAssets(): List<CexAssetRaw> {
        return api.getBalances(authToken, secret)
            .map {
                val decimals = 8
                val assetId = it.currency.iso3
                val depositEnabled = it.currency.refill == 1
                val withdrawEnabled = it.currency.withdraw == 1
                CexAssetRaw(
                    id = assetId,
                    accountId = account.id,
                    name = it.currency.name,
                    freeBalance = it.balance_available.movePointLeft(decimals),
                    lockedBalance = (it.balance - it.balance_available).movePointLeft(decimals),
                    depositEnabled = depositEnabled,
                    withdrawEnabled = withdrawEnabled,
                    networks = it.currency.networks.map { (_, networkId) ->
                        CexNetworkRaw(
                            network = networkId,
                            name = networkId,
                            isDefault = false,
                            depositEnabled = depositEnabled,
                            withdrawEnabled = withdrawEnabled,
                            blockchainUid = blockchainUidMap[networkId],
                        )
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
                    withdrawEnabled = it.withdrawAllEnable,
                    networks = it.networkList.map { coinNetwork ->
                        CexNetworkRaw(
                            network = coinNetwork.network,
                            name = coinNetwork.name,
                            isDefault = coinNetwork.isDefault,
                            depositEnabled = coinNetwork.depositEnable,
                            withdrawEnabled = coinNetwork.withdrawEnable,
                            blockchainUid = blockchainUidMap[coinNetwork.network],
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
    )

    data class DepositAddress(
        val address: String,
        val coin: String,
        val tag: String,
        val url: String,
    )

}