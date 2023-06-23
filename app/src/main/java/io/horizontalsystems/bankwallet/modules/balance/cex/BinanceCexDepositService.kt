package io.horizontalsystems.bankwallet.modules.balance.cex

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.modules.depositcex.DepositCexModule
import io.horizontalsystems.bankwallet.modules.market.ImageSource

class BinanceCexDepositService(apiKey: String, secretKey: String) : ICexDepositService {
    private val coinMapper = BinanceCexCoinMapper(App.marketKit)

    private val client = SpotClientImpl(apiKey, secretKey)
    private val wallet = client.createWallet()
    private val gson = Gson()

    override suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem> {
        val coinInfo = gson.fromJson<List<CoinInfo>>(
            wallet.coinInfo(mutableMapOf()),
            object : TypeToken<List<CoinInfo>>() {}.type
        )

        return coinInfo.map {
            val coin = coinMapper.getCoin(it.coin)
            val coinIconUrl = if (coin.isCustom) null else coin.imageUrl
            DepositCexModule.CexCoinViewItem(
                title = it.coin,
                subtitle = it.name,
                coinIconUrl = coinIconUrl,
                coinIconPlaceholder = R.drawable.coin_placeholder,
                assetId = it.coin
            )
        }
    }

    override suspend fun getNetworks(assetId: String): List<DepositCexModule.NetworkViewItem> {
        val coinInfo = gson.fromJson<List<CoinInfo>>(
            wallet.coinInfo(mutableMapOf()),
            object : TypeToken<List<CoinInfo>>() {}.type
        )

        return coinInfo
            .find { it.coin == assetId }
            ?.networkList
            ?.map {
                DepositCexModule.NetworkViewItem(
                    title = it.network,
                    imageSource = ImageSource.Local(R.drawable.fantom_erc20)
                )
            }
            ?: listOf()
    }

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

    data class CoinInfo(
        val coin: String,
        val name: String,
        val depositAllEnable: Boolean,
        val networkList: List<CoinNetwork>
    )

    data class CoinNetwork(
        val name: String,
        val network: String,
    )

    data class DepositAddress(
        val address: String,
        val coin: String,
        val tag: String,
        val url: String,
    )
}
