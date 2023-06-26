package cash.p.terminal.modules.balance.cex

import com.binance.connector.client.impl.SpotClientImpl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.imageUrl
import cash.p.terminal.core.isCustom
import cash.p.terminal.core.providers.BinanceCexProvider
import cash.p.terminal.modules.depositcex.DepositCexModule
import cash.p.terminal.modules.market.ImageSource

class BinanceCexDepositService(apiKey: String, secretKey: String) : ICexDepositService {
    private val coinMapper = BinanceCexCoinMapper(App.marketKit)

    private val client = SpotClientImpl(apiKey, secretKey)
    private val wallet = client.createWallet()
    private val gson = Gson()

    override suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem> {
        val coinInfo = gson.fromJson<List<BinanceCexProvider.CoinInfo>>(
            wallet.coinInfo(mutableMapOf()),
            object : TypeToken<List<BinanceCexProvider.CoinInfo>>() {}.type
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
        val coinInfo = gson.fromJson<List<BinanceCexProvider.CoinInfo>>(
            wallet.coinInfo(mutableMapOf()),
            object : TypeToken<List<BinanceCexProvider.CoinInfo>>() {}.type
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

        val depositAddress = gson.fromJson(wallet.depositAddress(params), BinanceCexProvider.DepositAddress::class.java)
        return CexAddress(depositAddress.address, depositAddress.tag)
    }
}
