package io.horizontalsystems.bankwallet.modules.balance.cex

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.isCustom
import io.horizontalsystems.bankwallet.modules.depositcex.DepositCexModule

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
                    coinUid = coin.uid
                )
            }
    }
}
