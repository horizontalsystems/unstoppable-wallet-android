package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.modules.depositcex.DepositCexModule
import io.horizontalsystems.bankwallet.modules.market.ImageSource

class CoinzixCexDepositService(
    private val authToken: String,
    private val secret: String
) : ICexDepositService {
    private val api = CoinzixCexApiService()
    private val coinMapper = ConzixCexCoinMapper(App.marketKit)

    override suspend fun getCoins(): List<DepositCexModule.CexCoinViewItem> {
        return api.getBalances(authToken, secret)
            .map {
                val coin = coinMapper.getCoin(it.currency)
                val coinIconUrl = if (coin.isCustom) null else coin.imageUrl
                DepositCexModule.CexCoinViewItem(
                    title = it.currency.iso3,
                    subtitle = it.currency.name,
                    coinIconUrl = coinIconUrl,
                    coinIconPlaceholder = R.drawable.coin_placeholder,
                    assetId = it.currency.iso3
                )
            }
    }

    override suspend fun getNetworks(assetId: String): List<DepositCexModule.NetworkViewItem> {
        val networks = api.getBalances(authToken, secret)
            .find {
                it.currency.iso3 == assetId
            }
            ?.currency
            ?.networks

        return networks?.map {
            DepositCexModule.NetworkViewItem(
                title = it.value,
                imageSource = ImageSource.Local(R.drawable.fantom_erc20)
            )
        } ?: listOf()
    }

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
}
